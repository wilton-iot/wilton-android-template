/*
 * Copyright 2019, alex at staticlibs.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wilton.android;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import wilton.WiltonException;

import static wilton.support.common.Utils.closeQuietly;
import static wilton.support.common.Utils.copy;
import static wilton.support.common.Utils.readZipEntryToString;

public class Common {

    public static String jsonDyload(File libDir, String name) {
        return
        "{\n" +
        "    \"name\": \"" + name + "\",\n" +
        "    \"directory\": \"" + libDir.getAbsolutePath() + "\"\n" +
        "}";
    }

    public static String jsonWiltonConfig(File filesDir, File libDir) {
        return jsonWiltonConfig(filesDir, libDir, "", "");
    }

    public static String jsonWiltonConfig(File filesDir, File libDir, String rootModuleName,
            String rootModulePath) {
        File stdlib = new File(filesDir, "std.wlib");
        String entry = "wilton-requirejs/wilton-packages.json";
        String packages = readZipEntryToString(stdlib, entry);
        String appPath = "";
        if (!rootModulePath.isEmpty()) {
            appPath = 
        "            \"" + rootModuleName + "\": \"file://" + rootModulePath + "\"\n";
        }
        return
        "{\n" +
        "    \"defaultScriptEngine\": \"duktape\",\n" +
        "    \"applicationDirectory\": \"" + filesDir.getAbsolutePath() + "/\",\n" +
        "    \"wiltonHome\": \"" + filesDir.getAbsolutePath() + "/\",\n" +
        "    \"android\": {\n" + 
        "         \"nativeLibsDir\": \"" + libDir.getAbsolutePath() + "\"\n" +
        "    },\n" +
        "    \"environmentVariables\": {\n" + 
        "         \"ANDROID\": true\n" +
        "    },\n" +
        "    \"requireJs\": {\n" +
        "        \"waitSeconds\": 0,\n" +
        "        \"enforceDefine\": true,\n" +
        "        \"nodeIdCompat\": true,\n" +
        "        \"baseUrl\": \"zip://" + filesDir.getAbsolutePath() + "/std.wlib\",\n" +
        "        \"paths\": {\n" +
                    appPath +
        "        },\n" +
        "        \"packages\": " + packages +
        "    \n},\n" +
        "    \"compileTimeOS\": \"android\"\n" +
        "}";
    }

    public static String jsonRunscript(String module, String func, String... args) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("\"");
            sb.append(args[i]);
            sb.append("\"");
        }
        sb.append("]");
        if (!func.isEmpty()) {
            return 
            "{\n" +
            "    \"module\": \"" + module + "\",\n" +
            "    \"func\": \"" + func + "\",\n" +
            "    \"args\": " + sb.toString() + "\n" +
            "}";
        } else {
            return 
            "{\n" +
            "    \"module\": \"" + module + "\",\n" +
            "    \"args\": " + sb.toString() + "\n" +
            "}";
        }
    }

    public static void unpackAsset(Context ctx, File filesDir, String assetName) {
        File destFile = new File(filesDir, assetName);
        if (destFile.exists()) return;
        InputStream is = null;
        OutputStream os = null;
        try {
            is = ctx.getAssets().open(assetName);
            os = new FileOutputStream(destFile);
            copy(is, os);
            os.close();
        } catch (IOException e) {
            throw new WiltonException("Error unpacking asset, name: [" + assetName + "]", e);
        } finally {
            closeQuietly(is);
            closeQuietly(os);
        }
    }


}
