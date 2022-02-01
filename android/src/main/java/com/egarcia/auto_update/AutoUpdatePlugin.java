package com.egarcia.auto_update;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.annotation.NonNull;

import java.io.File;
import java.util.Objects;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.plugin.common.MethodChannel.Result;

/** AutoUpdatePlugin */
public class AutoUpdatePlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
  private MethodChannel channel;
  private Context context;
  private Activity activity;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "auto_update");
    channel.setMethodCallHandler(this);
    context = flutterPluginBinding.getApplicationContext();
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    switch (call.method) {
      case "fetchGithubApk":
        fetchGithub(
                Objects.requireNonNull(call.argument("user")),
                Objects.requireNonNull(call.argument("packageName")),
                "application/vnd.android.package-archive"
                , result
        );
        break;
      case "downloadAndUpdate":
        downloadAndUpdate(Uri.parse(Objects.requireNonNull(call.argument("url"))), result);
        break;
      case "closeActivity":
        activity.finish();
        break;
      default:
        result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  private void updateApp(@NonNull Uri uri){
    context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    activity = binding.getActivity();
  }
  @Override
  public void onDetachedFromActivity() {
    activity.finish();
  }
  @Override
  public void onDetachedFromActivityForConfigChanges() {}
  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    activity = binding.getActivity();
  }

  public static String getApplicationName(Context context) {
    ApplicationInfo applicationInfo = context.getApplicationInfo();
    int stringId = applicationInfo.labelRes;
    return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);
  }

  private void fetchGithub(@NonNull String user, @NonNull String packageName,
                                       @NonNull String type, @NonNull Result result){
    String appPackageName = AutoUpdatePlugin.getApplicationName(context.getApplicationContext())
            .replaceAll(" ", "") + ".apk";
    String versionName;

    try {
      versionName = context.getPackageManager()
                            .getPackageInfo(context.getPackageName(), 0).versionName;
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
      result.error(String.valueOf(e.hashCode()), e.getMessage(), e);
      return;
    }

    Github github = new Github(user, packageName, type, appPackageName, versionName);
    github.start();

    try {
      github.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
      result.error(String.valueOf(e.hashCode()), e.getMessage(), e);
    }

    switch (github.fetched) {
      case 0:
        result.success((new GithubResults()).toMap());
        break;
      case 1:
        result.success(github.githubResults.toMap());
        break;
      case 2:
        result.success(GithubResults.upToDate().toMap());
        break;
      case -1:
        result.error(
                String.valueOf(github.exception.hashCode()),
                github.exception.getMessage(), github.exception
        );
        break;
    }

  }

  private void downloadAndUpdate(@NonNull Uri url, @NonNull Result result){

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      if (!Environment.isExternalStorageManager()){
        Uri packageUri = Uri.parse("package:" + context.getApplicationContext().getPackageName());
        activity.startActivity(
                new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        );
        return;
      }
    } else if (
        ((ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) ||
        (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)) &&
        (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.READ_EXTERNAL_STORAGE) ||
        ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE))
    ) {
      ActivityCompat.requestPermissions(activity,
              new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
      return;
    }


    String destination =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" +
                    AutoUpdatePlugin.getApplicationName(context.getApplicationContext())
                            .replaceAll(" ", "") + ".apk";
    final Uri uri = Uri.parse("file://" + destination);

    //Delete update file if exists
    File file = new File(destination);

    if (file.exists())
        file.delete();

    FileDownloader fileDownloader = new FileDownloader(url.toString(), file);
    fileDownloader.start();
    try {
      fileDownloader.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
      result.error(String.valueOf(e.hashCode()), e.getMessage(), e);
      return;
    }

    switch (fileDownloader.downloaded) {
      case 0:
        result.success(-1);
        return;
      case -1:
        result.error("1", fileDownloader.exception.getMessage(), fileDownloader.exception);
        return;
    }

    Intent install = new Intent(Intent.ACTION_VIEW);
    install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    install.addCategory(Intent.CATEGORY_DEFAULT);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      if (!activity.getPackageManager().canRequestPackageInstalls()) {
        Uri packageUri = Uri.parse("package:" + context.getApplicationContext().getPackageName());
        activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageUri));
        return;
      }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      Uri data = FileProvider.getUriForFile(
              context, context.getApplicationContext().getPackageName() + ".provider", file);
      install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      install.setDataAndType(data, "application/vnd.android.package-archive");
    } else {
      install.setDataAndType(uri, "application/vnd.android.package-archive");
    }

    try {
      activity.startActivity(install);
    } catch (ActivityNotFoundException e) {
      Log.d("-1", "No APP found to open this file。");
    } catch (Exception e) {
      Log.d("-4", "File opened incorrectly。");
    }

    activity.startActivity(install);
    activity.finish();
  }
}
