package com.reactnativesslpinning;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;

import org.json.JSONException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.CertificatePinner;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

@ReactModule(name = SslpinningModule.NAME)
public class SslpinningModule extends ReactContextBaseJavaModule {
  public static final String NAME = "Sslpinning";
  private ReactApplicationContext context;
  private static String content_type = "application/json; charset=utf-8";
  public static MediaType mediaType = MediaType.parse(content_type);
  String responseBodyString = "{}";

  public SslpinningModule(ReactApplicationContext reactContext) {
    super(reactContext);
    context = reactContext;
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  @ReactMethod
  public void fetch(String url, final ReadableMap options, Callback callback) {
    if (!isValidUrl(url)) {
      callback.invoke(null, "url is invalid!");
      return;
    }

    String hostname;
    try {
      hostname = getHostname(url);
    } catch (URISyntaxException e) {
      hostname = url;
    }

    if (!options.hasKey("certificates")) {
      callback.invoke(null, "certificates is required!");
      return;
    }

    ReadableArray hashes = options.getArray("certificates");
    if (hashes == null || hashes.size() == 0) {
      callback.invoke(null, "certificates is empty!");
      return;
    }

    try {
      CertificatePinner certificatePinner = getCertificatePinner(hostname, options);
      OkHttpClient client = getClient(options, certificatePinner);

      Headers header = setHeader(options);
      RequestBody body = setBody(context, options);
      String method = getMethod(options);

      Request request = new Request.Builder()
        .url(url)
        .headers(header)
        .method(method, body)
        .build();

      WritableMap output = Arguments.createMap();

      try {
        Response response = client.newCall(request).execute();
        int responseCode = response.code();

        byte[] bytes = response.body().bytes();
        responseBodyString = new String(bytes, "UTF-8");

        output.putInt("status", responseCode);
        output.putString("url", request.url().toString());

        if (!response.isSuccessful() || responseCode >= 400) {
          output.putString("error", responseBodyString);
          callback.invoke(null, output);
          return;
        }
        output.putString("response", responseBodyString);
        callback.invoke(output, null);
      } catch (IOException e) {
        output.putString("error", responseBodyString);

        callback.invoke(null, output);
        if (e instanceof java.net.SocketTimeoutException) {
          System.err.print("Socket TimeOut");
        } else {
          e.printStackTrace();
        }
      }
    } catch(JSONException e) {
      callback.invoke(null, e);
    }
  }

  private CertificatePinner getCertificatePinner(String hostname, ReadableMap options) {
    CertificatePinner.Builder certificatePinner = new CertificatePinner.Builder();

    ReadableArray hashes = options.getArray("certificates");
    for (int i = 0; i < hashes.size(); i++) {
      certificatePinner.add(hostname, "sha256/" + hashes.getString(i));
    }

    return certificatePinner.build();
  }

  private OkHttpClient getClient(ReadableMap options, CertificatePinner certificatePinner) throws JSONException {
      if (options.hasKey("timeout")) {
        int timeout = options.getInt("timeout");
        return new OkHttpClient.Builder()
          .connectTimeout(timeout, TimeUnit.MILLISECONDS)
          .readTimeout(timeout, TimeUnit.MILLISECONDS)
          .writeTimeout(timeout, TimeUnit.MILLISECONDS)
          .certificatePinner(certificatePinner)
          .build();
      } else {
        return new OkHttpClient.Builder()
          .certificatePinner(certificatePinner)
          .build();
      }
  }

  private static String getHostname(String url) throws URISyntaxException {
    URI uri = new URI(url);
    String domain = uri.getHost();
    return domain.startsWith("www.") ? domain.substring(4) : domain;
  }

  private String getMethod(ReadableMap options) {
    String method = "GET";
    if (options.hasKey("method")) {
      method = options.getString("method");
    }

    return method;
  }

  private Headers setHeader(ReadableMap options) {
    if (!options.hasKey("headers")) {
      return null;
    }

    ReadableMap headers = options.getMap("headers");
    Headers.Builder builder = new Headers.Builder();

    Map<String, String> headersMap = readableMapToHashMap(headers);
    for (Map.Entry<String, String> set : headersMap.entrySet()) {
      builder.add(set.getKey(), set.getValue());
    }

    return builder.build();
  }

  private HashMap readableMapToHashMap(ReadableMap readableMap) {
    if (readableMap == null) {
      return null;
    }

    HashMap map = new HashMap<String, String>();
    ReadableMapKeySetIterator keySetIterator = readableMap.keySetIterator();
    while (keySetIterator.hasNextKey()) {
      String key = keySetIterator.nextKey();
      ReadableType type = readableMap.getType(key);
      switch(type) {
        case String:
          map.put(key, readableMap.getString(key));
          break;
        case Map:
          HashMap<String, Object> attributes = this.readableMapToHashMap(readableMap.getMap(key));
          map.put(key, attributes);
          break;
        default:
          // do nothing
      }
    }

    return map;
  }

  private static boolean isFilePart(ReadableArray part) {
    if (part.getType(1) != ReadableType.Map) {
      return false;
    }
    ReadableMap value = part.getMap(1);
    return value.hasKey("type") && (value.hasKey("uri") || value.hasKey("path"));
  }

  private RequestBody setBody(ReactApplicationContext context, ReadableMap options) {
    if (!options.hasKey("body")) {
      return null;
    }

    RequestBody body = null;
    ReadableType bodyType = options.getType("body");
      switch (bodyType) {
        case String:
          body = RequestBody.create(mediaType, options.getString("body"));
          break;
        case Map:
          ReadableMap bodyMap = options.getMap("body");
          if (bodyMap.hasKey("formData")) {
            ReadableMap formData = bodyMap.getMap("formData");
            body = getBody(formData);
          } else if (bodyMap.hasKey("_parts")) {
            body = getBody(bodyMap);
          }
          break;
    }
    return body;
  }

  private RequestBody getBody(ReadableMap body) {
    MultipartBody.Builder multipartBodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
    multipartBodyBuilder.setType((MediaType.parse("multipart/form-data")));
    if (body.hasKey("_parts")) {
      ReadableArray parts = body.getArray("_parts");
      for (int i = 0; i < parts.size(); i++) {
        ReadableArray part = parts.getArray(i);
        String key = "";
        if (part.getType(0) == ReadableType.String) {
          key = part.getString(0);
        } else if (part.getType(0) == ReadableType.Number) {
          key = String.valueOf(part.getInt(0));
        }

        if (isFilePart(part)) {
          ReadableMap fileData = part.getMap(1);
          addFormDataPart(this.context, multipartBodyBuilder, fileData, key);
        } else {
          String value = part.getString(1);
          multipartBodyBuilder.addFormDataPart(key, value);
        }
      }
    }
    return multipartBodyBuilder.build();
  }

  private static void addFormDataPart(Context context, MultipartBody.Builder multipartBodyBuilder, ReadableMap fileData, String key) {
    Uri _uri = Uri.parse("");
    if (fileData.hasKey("uri")) {
      _uri = Uri.parse(fileData.getString("uri"));
    } else if (fileData.hasKey("path")) {
      _uri = Uri.parse(fileData.getString("path"));
    }
    String type = fileData.getString("type");
    String fileName = "";
    if (fileData.hasKey("fileName")) {
      fileName = fileData.getString("fileName");
    } else if (fileData.hasKey("name")) {
      fileName = fileData.getString("name");
    }

    try {
      File file = getTempFile(context, _uri);
      multipartBodyBuilder.addFormDataPart(key, fileName, RequestBody.create(MediaType.parse(type), file));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static File getTempFile(Context context, Uri uri) throws IOException {
    File file = File.createTempFile("media", null);
    InputStream inputStream = context.getContentResolver().openInputStream(uri);
    OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
    byte[] buffer = new byte[1024];
    int len;
    while ((len = inputStream.read(buffer)) != -1)
      outputStream.write(buffer, 0, len);
    inputStream.close();
    outputStream.close();
    return file;
  }

  private boolean isValidUrl(String url) {
    String regex = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
    return url.matches(regex);
  }
}
