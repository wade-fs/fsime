// FsimeTextService.cpp — Minimal TSF Text Service skeleton for FsimeIME.
//
// Architecture:
//   Windows TSF ──keystrokes──► FsimeTextService (DLL, this file)
//                                      │  Named Pipe (JSON)
//                               FsimeServer.exe (Go, backend)
//                                      │
//                               b.db (SQLite, shared with Android)
//
// This file is intentionally kept thin:
//   - Register ITfTextInputProcessor with TSF
//   - Forward key events to the Go server via PipeClient
//   - Display composition / candidates using ITfCompositionSink and a candidate window
//
// To build on Windows (Visual Studio):
//   cl /EHsc /LD /DUNICODE FsimeTextService.cpp PipeClient.cpp
//      /link ole32.lib oleaut32.lib /OUT:FsimeIME.dll
//
// Or cross-compile via MinGW-w64 on Ubuntu (see Makefile).

#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <msctf.h>
#include <olectl.h>
#include <string>
#include <vector>
#include <sstream>
#include "PipeClient.h"

// ---------------------------------------------------------------------------
// GUIDs — generate unique values with uuidgen for your own IME
// ---------------------------------------------------------------------------
// {CLSID_FsimeTextService}
static const CLSID CLSID_FsimeTextService =
    { 0xa1b2c3d4, 0xe5f6, 0x7890, {0xab, 0xcd, 0xef, 0x01, 0x23, 0x45, 0x67, 0x89} };

// Language profile GUID
static const GUID GUID_Profile =
    { 0xb2c3d4e5, 0xf6a7, 0x8901, {0xbc, 0xde, 0xf0, 0x12, 0x34, 0x56, 0x78, 0x9a} };

// ---------------------------------------------------------------------------
// Helper: build a JSON request string
// ---------------------------------------------------------------------------
static std::string MakeKeyRequest(const std::string& key) {
    return "{\"type\":\"key\",\"key\":\"" + key + "\"}";
}
static std::string MakeSelectRequest(int index) {
    std::ostringstream ss;
    ss << "{\"type\":\"select\",\"index\":" << index << "}";
    return ss.str();
}
static std::string MakeResetRequest() {
    return "{\"type\":\"reset\"}";
}

// ---------------------------------------------------------------------------
// FsimeTextService — implements ITfTextInputProcessor + ITfKeyEventSink
// ---------------------------------------------------------------------------
class FsimeTextService
    : public ITfTextInputProcessor
    , public ITfKeyEventSink
{
public:
    // TF_CLIENTID_NULL == 0 (MSVC SDK constant, not in MinGW headers)
    static constexpr TfClientId kNullClientId = 0;

    FsimeTextService() : refCount_(1), threadMgr_(nullptr), clientId_(kNullClientId) {}
    virtual ~FsimeTextService() {
        if (threadMgr_) threadMgr_->Release();
    }

    // ---- IUnknown ----
    STDMETHODIMP QueryInterface(REFIID riid, void** ppv) override {
        if (riid == IID_IUnknown || riid == IID_ITfTextInputProcessor) {
            *ppv = static_cast<ITfTextInputProcessor*>(this);
        } else if (riid == IID_ITfKeyEventSink) {
            *ppv = static_cast<ITfKeyEventSink*>(this);
        } else {
            *ppv = nullptr;
            return E_NOINTERFACE;
        }
        AddRef();
        return S_OK;
    }
    STDMETHODIMP_(ULONG) AddRef()  override { return ++refCount_; }
    STDMETHODIMP_(ULONG) Release() override {
        ULONG r = --refCount_;
        if (r == 0) delete this;
        return r;
    }

    // ---- ITfTextInputProcessor ----
    STDMETHODIMP Activate(ITfThreadMgr* pThreadMgr, TfClientId tid) override {
        threadMgr_ = pThreadMgr;
        threadMgr_->AddRef();
        clientId_ = tid;
        pipe_.Connect(); // best-effort: server may not be up yet

        // Register key event sink
        ITfKeystrokeMgr* keystrokeMgr = nullptr;
        if (SUCCEEDED(threadMgr_->QueryInterface(IID_ITfKeystrokeMgr,
                                                  (void**)&keystrokeMgr))) {
            keystrokeMgr->AdviseKeyEventSink(clientId_,
                static_cast<ITfKeyEventSink*>(this), TRUE);
            keystrokeMgr->Release();
        }
        return S_OK;
    }

    STDMETHODIMP Deactivate() override {
        ITfKeystrokeMgr* keystrokeMgr = nullptr;
        if (threadMgr_ && SUCCEEDED(threadMgr_->QueryInterface(
                IID_ITfKeystrokeMgr, (void**)&keystrokeMgr))) {
            keystrokeMgr->UnadviseKeyEventSink(clientId_);
            keystrokeMgr->Release();
        }
        pipe_.Disconnect();
        if (threadMgr_) { threadMgr_->Release(); threadMgr_ = nullptr; }
        clientId_ = kNullClientId;
        return S_OK;
    }

    // ---- ITfKeyEventSink ----
    STDMETHODIMP OnSetFocus(BOOL /*fForeground*/) override { return S_OK; }

    STDMETHODIMP OnTestKeyDown(ITfContext* /*pCtx*/, WPARAM wParam,
                               LPARAM /*lParam*/, BOOL* pEaten) override {
        *pEaten = ShouldEat(wParam) ? TRUE : FALSE;
        return S_OK;
    }

    STDMETHODIMP OnKeyDown(ITfContext* pCtx, WPARAM wParam,
                           LPARAM /*lParam*/, BOOL* pEaten) override {
        *pEaten = FALSE;
        if (!ShouldEat(wParam)) return S_OK;

        std::string req;
        if (wParam == VK_BACK) {
            req = MakeKeyRequest("BackSpace");
        } else if (wParam == VK_ESCAPE) {
            req = MakeResetRequest();
        } else if (wParam >= '0' && wParam <= '9') {
            // Number keys: select candidate
            int idx = (wParam == '0') ? 9 : (wParam - '1');
            req = MakeSelectRequest(idx);
        } else {
            char keyChar = MapVkToChar(wParam);
            if (keyChar == 0) return S_OK;
            req = MakeKeyRequest(std::string(1, keyChar));
        }

        FsimeResponse resp = pipe_.SendRequest(req);
        HandleResponse(pCtx, resp);
        *pEaten = TRUE;
        return S_OK;
    }

    STDMETHODIMP OnTestKeyUp(ITfContext*, WPARAM, LPARAM, BOOL* pEaten) override {
        *pEaten = FALSE; return S_OK;
    }
    STDMETHODIMP OnKeyUp(ITfContext*, WPARAM, LPARAM, BOOL* pEaten) override {
        *pEaten = FALSE; return S_OK;
    }
    STDMETHODIMP OnPreservedKey(ITfContext*, REFGUID, BOOL* pEaten) override {
        *pEaten = FALSE; return S_OK;
    }

private:
    LONG           refCount_;
    ITfThreadMgr*  threadMgr_;
    TfClientId     clientId_;
    PipeClient     pipe_;

    // Decide whether to intercept this key.
    bool ShouldEat(WPARAM vk) {
        if (vk == VK_BACK || vk == VK_ESCAPE) return true;
        // Only eat printable ASCII (a-z, A-Z, digits, common punctuation)
        if (vk >= 'A' && vk <= 'Z') return true;
        if (vk >= '0' && vk <= '9') return true;
        return false;
    }

    char MapVkToChar(WPARAM vk) {
        BYTE keyState[256] = {};
        GetKeyboardState(keyState);
        WCHAR buf[4] = {};
        int n = ToUnicode((UINT)vk, 0, keyState, buf, 4, 0);
        if (n <= 0) return 0;
        // Only accept ASCII printable range
        if (buf[0] > 0 && buf[0] < 128) return (char)buf[0];
        return 0;
    }

    // Update composition window and commit if needed.
    void HandleResponse(ITfContext* pCtx, const FsimeResponse& resp) {
        if (!resp.commit.empty()) {
            CommitText(pCtx, resp.commit);
        }
        // TODO: Update composition display (ITfComposition) and candidate window
        // This requires additional TSF sink registration (ITfCompositionSink).
        // For the skeleton, the commit path is functional.
    }

    void CommitText(ITfContext* pCtx, const std::wstring& text) {
        // Obtain an edit session to insert text into the document.
        // Full implementation requires ITfEditSession subclass.
        // Placeholder: use SendInput as a fallback for testing.
        (void)pCtx;
        for (wchar_t ch : text) {
            INPUT inp = {};
            inp.type = INPUT_KEYBOARD;
            inp.ki.wVk       = 0;
            inp.ki.wScan     = ch;
            inp.ki.dwFlags   = KEYEVENTF_UNICODE;
            SendInput(1, &inp, sizeof(INPUT));
            inp.ki.dwFlags = KEYEVENTF_UNICODE | KEYEVENTF_KEYUP;
            SendInput(1, &inp, sizeof(INPUT));
        }
    }
};

// ---------------------------------------------------------------------------
// Class Factory
// ---------------------------------------------------------------------------
class FsimeClassFactory : public IClassFactory {
    LONG ref_ = 1;
public:
    virtual ~FsimeClassFactory() = default;
    STDMETHODIMP QueryInterface(REFIID riid, void** ppv) override {
        if (riid == IID_IUnknown || riid == IID_IClassFactory) {
            *ppv = this; AddRef(); return S_OK;
        }
        *ppv = nullptr; return E_NOINTERFACE;
    }
    STDMETHODIMP_(ULONG) AddRef()  override { return ++ref_; }
    STDMETHODIMP_(ULONG) Release() override { ULONG r=--ref_; if(!r)delete this; return r; }

    STDMETHODIMP CreateInstance(IUnknown* outer, REFIID riid, void** ppv) override {
        if (outer) return CLASS_E_NOAGGREGATION;
        auto* svc = new FsimeTextService();
        HRESULT hr = svc->QueryInterface(riid, ppv);
        svc->Release();
        return hr;
    }
    STDMETHODIMP LockServer(BOOL) override { return S_OK; }
};

// ---------------------------------------------------------------------------
// DLL entry points
// ---------------------------------------------------------------------------
BOOL WINAPI DllMain(HINSTANCE hInst, DWORD reason, LPVOID) {
    (void)hInst; (void)reason;
    return TRUE;
}

STDAPI DllGetClassObject(REFCLSID rclsid, REFIID riid, LPVOID* ppv) {
    if (rclsid != CLSID_FsimeTextService) return CLASS_E_CLASSNOTAVAILABLE;
    auto* factory = new FsimeClassFactory();
    HRESULT hr = factory->QueryInterface(riid, ppv);
    factory->Release();
    return hr;
}

STDAPI DllCanUnloadNow() { return S_FALSE; }

STDAPI DllRegisterServer() {
    // TODO: register CLSID, language profile via ITfInputProcessorProfiles
    // See PIME's Register.cpp for a complete implementation.
    return S_OK;
}

STDAPI DllUnregisterServer() { return S_OK; }
