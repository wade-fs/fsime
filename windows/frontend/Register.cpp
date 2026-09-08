#include <windows.h>
#include <msctf.h>
#include <olectl.h>
#include <strsafe.h>

extern HINSTANCE g_hInst;
extern const CLSID CLSID_FsimeTextService;
extern const GUID GUID_Profile;

const WCHAR TEXTSERVICE_DESC[] = L"混瞎輸入法 (FsimeIME)";

// Converts GUID to string
void CLSIDToString(REFGUID refGUID, WCHAR *pCLSIDString) {
    StringFromGUID2(refGUID, pCLSIDString, 39);
}

// Sets a registry key
BOOL SetRegKey(HKEY hKey, const WCHAR *lpszSubKey, const WCHAR *lpszValueName, const WCHAR *lpszValueData) {
    HKEY hSubKey;
    if (RegCreateKeyExW(hKey, lpszSubKey, 0, NULL, REG_OPTION_NON_VOLATILE, KEY_WRITE, NULL, &hSubKey, NULL) != ERROR_SUCCESS) {
        return FALSE;
    }
    if (lpszValueData) {
        RegSetValueExW(hSubKey, lpszValueName, 0, REG_SZ, (const BYTE*)lpszValueData, (lstrlenW(lpszValueData) + 1) * sizeof(WCHAR));
    }
    RegCloseKey(hSubKey);
    return TRUE;
}

STDAPI RegisterServer() {
    WCHAR szModulePath[MAX_PATH];
    GetModuleFileNameW(g_hInst, szModulePath, MAX_PATH);

    WCHAR szCLSID[39];
    CLSIDToString(CLSID_FsimeTextService, szCLSID);

    WCHAR szKey[256];
    StringCchPrintfW(szKey, ARRAYSIZE(szKey), L"CLSID\\%s", szCLSID);
    SetRegKey(HKEY_CLASSES_ROOT, szKey, NULL, TEXTSERVICE_DESC);

    StringCchPrintfW(szKey, ARRAYSIZE(szKey), L"CLSID\\%s\\InProcServer32", szCLSID);
    SetRegKey(HKEY_CLASSES_ROOT, szKey, NULL, szModulePath);
    SetRegKey(HKEY_CLASSES_ROOT, szKey, L"ThreadingModel", L"Apartment");

    ITfInputProcessorProfiles *pProfiles;
    if (SUCCEEDED(CoCreateInstance(CLSID_TF_InputProcessorProfiles, NULL, CLSCTX_INPROC_SERVER, IID_ITfInputProcessorProfiles, (void**)&pProfiles))) {
        pProfiles->Register(CLSID_FsimeTextService);
        
        // 0x0404 is zh-TW
        pProfiles->AddLanguageProfile(CLSID_FsimeTextService, 0x0404, GUID_Profile, TEXTSERVICE_DESC, (ULONG)wcslen(TEXTSERVICE_DESC), szModulePath, (ULONG)wcslen(szModulePath), 0);
        pProfiles->Release();
    }

    ITfCategoryMgr *pCategoryMgr;
    if (SUCCEEDED(CoCreateInstance(CLSID_TF_CategoryMgr, NULL, CLSCTX_INPROC_SERVER, IID_ITfCategoryMgr, (void**)&pCategoryMgr))) {
        pCategoryMgr->RegisterCategory(CLSID_FsimeTextService, GUID_TFCAT_TIP_KEYBOARD, CLSID_FsimeTextService);
        pCategoryMgr->Release();
    }
    return S_OK;
}

STDAPI UnregisterServer() {
    ITfInputProcessorProfiles *pProfiles;
    if (SUCCEEDED(CoCreateInstance(CLSID_TF_InputProcessorProfiles, NULL, CLSCTX_INPROC_SERVER, IID_ITfInputProcessorProfiles, (void**)&pProfiles))) {
        pProfiles->Unregister(CLSID_FsimeTextService);
        pProfiles->Release();
    }

    ITfCategoryMgr *pCategoryMgr;
    if (SUCCEEDED(CoCreateInstance(CLSID_TF_CategoryMgr, NULL, CLSCTX_INPROC_SERVER, IID_ITfCategoryMgr, (void**)&pCategoryMgr))) {
        pCategoryMgr->UnregisterCategory(CLSID_FsimeTextService, GUID_TFCAT_TIP_KEYBOARD, CLSID_FsimeTextService);
        pCategoryMgr->Release();
    }

    WCHAR szCLSID[39];
    CLSIDToString(CLSID_FsimeTextService, szCLSID);
    WCHAR szKey[256];
    StringCchPrintfW(szKey, ARRAYSIZE(szKey), L"CLSID\\%s\\InProcServer32", szCLSID);
    RegDeleteKeyW(HKEY_CLASSES_ROOT, szKey);
    StringCchPrintfW(szKey, ARRAYSIZE(szKey), L"CLSID\\%s", szCLSID);
    RegDeleteKeyW(HKEY_CLASSES_ROOT, szKey);

    return S_OK;
}
