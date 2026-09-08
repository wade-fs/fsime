; fsime.iss — FsimeIME Windows 輸入法安裝腳本
; 使用 Inno Setup 6.4.0 編譯
; 在 Ubuntu 上執行方式:
;   wine ../inno-setup-6.4.0/ISCC.exe fsime.iss
; 或直接用 Makefile:
;   make -C .. installer

#define AppName      "混瞎輸入法 (FsimeIME)"
#define AppNameEn    "FsimeIME"
#define AppVersion   "1.0.0"
#define AppPublisher "Wade"
#define AppURL       "https://github.com/wade/fsime"
#define AppExeName   "fsime-server.exe"
#define DllName      "FsimeIME.dll"
#define DbName       "b.db"

; 來源目錄 (相對於此 .iss 所在的 windows/scripts/)
#define SrcDir       "..\dist"
#define InnoDir      "..\inno-setup-6.4.0"

[Setup]
AppId={{A1B2C3D4-E5F6-7890-ABCD-EF0123456789}
AppName={#AppName}
AppVersion={#AppVersion}
AppVerName={#AppName} {#AppVersion}
AppPublisher={#AppPublisher}
AppPublisherURL={#AppURL}
AppSupportURL={#AppURL}
AppUpdatesURL={#AppURL}

; 安裝目標：64 位元 Program Files
DefaultDirName={autopf64}\{#AppNameEn}
DefaultGroupName={#AppName}
DisableProgramGroupPage=yes

; 只允許 x64 Windows
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible

; 輸出
OutputDir=..\output
OutputBaseFilename=FsimeIME-{#AppVersion}-setup
SetupIconFile={#InnoDir}\SetupClassicIcon.ico

; 壓縮
Compression=lzma2/ultra64
SolidCompression=yes
LZMAUseSeparateProcess=yes

; UI
WizardStyle=modern
WizardResizable=yes
ShowLanguageDialog=no

; 安裝後不自動啟動（需要手動 regsvr32 或重新登入）
DisableFinishedPage=no

; 需要管理員（regsvr32 寫入 HKLM）
PrivilegesRequired=admin
PrivilegesRequiredOverridesAllowed=dialog

; 最低需求 Windows 7
MinVersion=6.1

; 語言（內建無繁中，用日文介面搭配英文 fallback）
[Languages]
Name: "japanese"; MessagesFile: "{#InnoDir}\Languages\Japanese.isl"
Name: "english";  MessagesFile: "{#InnoDir}\Default.isl"

; ─────────────────────────────────────────────
; 安裝檔案
; ─────────────────────────────────────────────
[Files]
; Go 後端伺服器
Source: "{#SrcDir}\{#AppExeName}"; DestDir: "{app}"; Flags: ignoreversion

; TSF DLL（需 regsvr32）
Source: "{#SrcDir}\{#DllName}";    DestDir: "{app}"; Flags: ignoreversion regserver

; 詞庫資料庫
Source: "{#SrcDir}\{#DbName}";     DestDir: "{app}"; Flags: ignoreversion

; ─────────────────────────────────────────────
; 登錄設定
; ─────────────────────────────────────────────
[Registry]
; 讓 Windows 知道後端 exe 的位置（供日後管理用）
Root: HKLM; Subkey: "SOFTWARE\{#AppNameEn}"; \
    ValueType: string; ValueName: "InstallDir"; \
    ValueData: "{app}"; Flags: uninsdeletekey

Root: HKLM; Subkey: "SOFTWARE\{#AppNameEn}"; \
    ValueType: string; ValueName: "Version"; \
    ValueData: "{#AppVersion}"

; ─────────────────────────────────────────────
; 開始功能表
; ─────────────────────────────────────────────
[Icons]
; 「手動啟動後端」捷徑（正式版應改成 Windows Service）
Name: "{group}\啟動 FsimeIME 後端";  Filename: "{app}\{#AppExeName}"; \
    Parameters: "-db ""{app}\{#DbName}"""; \
    Comment: "啟動 FsimeIME 後端伺服器"
Name: "{group}\解除安裝 {#AppName}"; Filename: "{uninstallexe}"

; ─────────────────────────────────────────────
; 執行（安裝後）
; ─────────────────────────────────────────────
[Run]
; DLL 已由 [Files] 的 regserver flag 處理，這裡再補一個 regsvr32 確保
Filename: "{sys}\regsvr32.exe"; \
    Parameters: "/s ""{app}\{#DllName}"""; \
    Flags: runhidden waituntilterminated; \
    StatusMsg: "正在登錄輸入法 DLL..."

; 安裝後啟動後端（可選，使用者可取消）
Filename: "{app}\{#AppExeName}"; \
    Parameters: "-db ""{app}\{#DbName}"""; \
    Flags: nowait postinstall skipifsilent unchecked; \
    Description: "立即啟動 FsimeIME 後端伺服器"

; ─────────────────────────────────────────────
; 解除安裝
; ─────────────────────────────────────────────
[UninstallRun]
; 先反向登錄 DLL
Filename: "{sys}\regsvr32.exe"; \
    Parameters: "/s /u ""{app}\{#DllName}"""; \
    Flags: runhidden waituntilterminated

; 結束後端行程（如果正在執行中）
Filename: "{sys}\taskkill.exe"; \
    Parameters: "/f /im {#AppExeName}"; \
    Flags: runhidden waituntilterminated

; ─────────────────────────────────────────────
; Pascal Script（安裝前檢查）
; ─────────────────────────────────────────────
[Code]
function InitializeSetup(): Boolean;
begin
  // 僅允許 64 位元 Windows
  if not Is64BitInstallMode then begin
    MsgBox('FsimeIME 需要 64 位元版 Windows（Windows 7 SP1 或更新版本）。', mbError, MB_OK);
    Result := False;
    Exit;
  end;
  Result := True;
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssPostInstall then begin
    // 後端路徑寫入登錄，供使用者自行設定開機啟動
    // 正式版應改用 Windows Service (sc create)
  end;
end;
