#include <windows.h>
#include <shlobj_core.h>

#include <string>
#include <vector>

#pragma comment(lib,"Version.lib") 

using namespace std;

struct _LANGCODEPAGE {
    int wLanguage;
    int wCodePage;
};

class AutoUpdate {
    private:
    static wchar_t* strtowstr(const std::string &str)
    {
        // Convert an ASCII string to a Unicode String
        wchar_t *wszTo = new wchar_t[str.length() + 1];
        wszTo[str.size()] = L'\0';
        MultiByteToWideChar(CP_ACP, 0, str.c_str(), -1, wszTo, (int)str.length());
        return wszTo;
    }
    static string wstrtostr(const wstring &wstr)
    {
        // Convert a Unicode string to an ASCII string
        string strTo;
        char *szTo = new char[wstr.length() + 1];
        szTo[wstr.size()] = '\0';
        WideCharToMultiByte(CP_ACP, 0, wstr.c_str(), -1, szTo, (int)wstr.length(), NULL, NULL);
        strTo = szTo;
        delete[] szTo;
        return strTo;
    }
    static string getKnownFolder(GUID folderID){
        wchar_t* pszPath = 0;
        SHGetKnownFolderPath(folderID, 0, NULL, &pszPath);
        wstring wstr(pszPath);
        string str = AutoUpdate::wstrtostr(wstr);
        CoTaskMemFree(static_cast<void*>(pszPath));
        return str;
    }
    static string int_to_hex(int w, size_t hex_len = 4) {
        static const char* digits = "0123456789ABCDEF";
        string rc(hex_len,'0');
        for (size_t i=0, j=(hex_len-1)*4 ; i<hex_len; ++i,j-=4)
            rc[i] = digits[(w>>j) & 0x0f];
        return rc;
    }
    public:
    static string getDownloadFolder(){
        return AutoUpdate::getKnownFolder(FOLDERID_Downloads);
    }
    static string getDocumentsFolder(){
        return AutoUpdate::getKnownFolder(FOLDERID_Documents);
    }
    static HINSTANCE runFileWindows(string filePath){
        string path = filePath.substr(0, filePath.rfind("\\"));
        wchar_t* lpFile = AutoUpdate::strtowstr(filePath);
        wchar_t* lpDirectory = AutoUpdate::strtowstr(path);

        HINSTANCE hins = ShellExecuteW(NULL, L"open", lpFile, NULL, lpDirectory, 0);

        delete[] lpFile;
        delete[] lpDirectory;

        return hins;
    }
    static bool getProductAndVersion(string & strProductName, string & strProductVersion){
        // get the filename of the executable containing the version resource
        TCHAR szFilename[MAX_PATH + 1] = {0};
        if (GetModuleFileName(NULL, szFilename, MAX_PATH) == 0) return false;

        // allocate a block of memory for the version info
        DWORD dummy;
        DWORD dwSize = GetFileVersionInfoSize(szFilename, &dummy);
        if (dwSize == 0) return false;
        vector<BYTE> data(dwSize);

        // load the version info
        if (!GetFileVersionInfo(szFilename, NULL, dwSize, &data[0])) return false;

        PUINT dummy_2 = 0;
        LPCWSTR lpSubBlock = L"\\VarFileInfo\\Translation";
        LPVOID lpTranslate = NULL;
        if (!VerQueryValue(&data[0], lpSubBlock, &lpTranslate, dummy_2)) return false;

        // get the name and version strings
        LPVOID pvProductName = NULL;
        unsigned int iProductNameLen = 0;
        LPVOID pvProductVersion = NULL;
        unsigned int iProductVersionLen = 0;

        struct _LANGCODEPAGE *langCodePage = (struct _LANGCODEPAGE *) lpTranslate;

        int langCodePages[4][2] = {
            langCodePage->wLanguage, langCodePage->wCodePage,
            GetUserDefaultLangID(), langCodePage->wCodePage,
            langCodePage->wLanguage, 1252,
            GetUserDefaultLangID(), 1252
        };

        string rs = "\\StringFileInfo\\";
        string productName = "\\ProductName";
        string productVersion = "\\ProductVersion";
        bool productNameVersion = false;
        for (int i = 0; i < 4; i++){
            string rsID = rs + AutoUpdate::int_to_hex(langCodePages[i][0]) + AutoUpdate::int_to_hex(langCodePages[i][1]);
            string rsIDProductName = rsID + productName;
            string rsIDProductVersion = rsID + productVersion;
            if (VerQueryValue(&data[0], wstring(rsIDProductName.begin(), rsIDProductName.end()).c_str(), &pvProductName, &iProductNameLen) &&
                VerQueryValue(&data[0], wstring(rsIDProductVersion.begin(), rsIDProductVersion.end()).c_str(), &pvProductVersion, &iProductVersionLen)) {
                    productNameVersion = true;
                    break;
            }
        }

        if (productNameVersion) {
            wstring wstrPN((LPWSTR) pvProductName);
            wstring wstrPV((LPWSTR) pvProductVersion);

            strProductName = AutoUpdate::wstrtostr(wstrPN);
            strProductVersion = AutoUpdate::wstrtostr(wstrPV);
        }

        return productNameVersion;
    }
};