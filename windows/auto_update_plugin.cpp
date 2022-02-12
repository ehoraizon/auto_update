#include "include/auto_update/auto_update_plugin.h"
#include "include/auto_update/auto_update.h"

// This must be included before many other Windows headers.
#include <windows.h>

// For getPlatformVersion; remove unless needed for your plugin implementation.
#include <VersionHelpers.h>

#include <flutter/method_channel.h>
#include <flutter/plugin_registrar_windows.h>
#include <flutter/standard_method_codec.h>

#include <map>
#include <memory>
#include <sstream>
#include <string>

namespace {

using flutter::EncodableList;
using flutter::EncodableValue;
using flutter::EncodableMap;

class AutoUpdatePlugin : public flutter::Plugin {
 public:
  static void RegisterWithRegistrar(flutter::PluginRegistrarWindows *registrar);

  AutoUpdatePlugin();

  virtual ~AutoUpdatePlugin();

 private:
  // Called when a method is called on this plugin's channel from Dart.
  void HandleMethodCall(
      const flutter::MethodCall<flutter::EncodableValue> &method_call,
      std::unique_ptr<flutter::MethodResult<flutter::EncodableValue>> result);
};

// static
void AutoUpdatePlugin::RegisterWithRegistrar(
    flutter::PluginRegistrarWindows *registrar) {
  auto channel =
      std::make_unique<flutter::MethodChannel<flutter::EncodableValue>>(
          registrar->messenger(), "auto_update",
          &flutter::StandardMethodCodec::GetInstance());

  auto plugin = std::make_unique<AutoUpdatePlugin>();

  channel->SetMethodCallHandler(
      [plugin_pointer = plugin.get()](const auto &call, auto result) {
        plugin_pointer->HandleMethodCall(call, std::move(result));
      });

  registrar->AddPlugin(std::move(plugin));
}

AutoUpdatePlugin::AutoUpdatePlugin() {}

AutoUpdatePlugin::~AutoUpdatePlugin() {}

void AutoUpdatePlugin::HandleMethodCall(
    const flutter::MethodCall<flutter::EncodableValue> &method_call,
    std::unique_ptr<flutter::MethodResult<flutter::EncodableValue>> result) {

  const auto *arguments = std::get_if<EncodableMap>(method_call.arguments());
  
  if (method_call.method_name().compare("getPlatformVersion") == 0) {
    std::ostringstream version_stream;
    version_stream << "Windows ";
    if (IsWindows10OrGreater()) {
      version_stream << "10+";
    } else if (IsWindows8OrGreater()) {
      version_stream << "8";
    } else if (IsWindows7OrGreater()) {
      version_stream << "7";
    }
    result->Success(flutter::EncodableValue(version_stream.str()));
  } else if (method_call.method_name().compare("getDownloadFolder") == 0) {
    result->Success(flutter::EncodableValue(AutoUpdate::getDownloadFolder()));
  } else if (method_call.method_name().compare("getDocumentsFolder") == 0) {
    result->Success(flutter::EncodableValue(AutoUpdate::getDocumentsFolder()));
  } else if (method_call.method_name().compare("getProductAndVersion") == 0) {
    string productName, productVersion;
    if (AutoUpdate::getProductAndVersion(productName, productVersion)){
      result->Success(flutter::EncodableList({productName, productVersion}));
    } else {
      result->Error("0", "Unable to find name and version of product.");
    }
  } else if (method_call.method_name().compare("runFileWindows") == 0) { 
    std::string filePath;
    if (arguments){
      auto filePath_it = arguments->find(EncodableValue("filePath"));
      if (filePath_it != arguments->end()){
        filePath = std::get<std::string>(filePath_it->second);
      }
      int64_t res = (int64_t) AutoUpdate::runFileWindows(filePath);
      result->Success(flutter::EncodableValue(res));
    } else {
      result->Error("0", "Unable to run file");
    }
  } else {
    result->NotImplemented();
  }
}

}  // namespace

void AutoUpdatePluginRegisterWithRegistrar(
    FlutterDesktopPluginRegistrarRef registrar) {
  AutoUpdatePlugin::RegisterWithRegistrar(
      flutter::PluginRegistrarManager::GetInstance()
          ->GetRegistrar<flutter::PluginRegistrarWindows>(registrar));
}
