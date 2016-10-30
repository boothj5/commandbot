/*
 * Copyright 2015 James Booth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.boothj5.minions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

class MinionJar {
    private static final String MANIFEST_CLASS = "MinionClass";
    private static final String MANIFEST_COMMAND = "MinionCommand" ;

    private final String name;
    private final long timestamp;
    private final URL url;
    private final String command;
    private final String className;

    MinionJar(File file) {
        name = file.getName();
        timestamp = file.lastModified();

        try (InputStream in = new FileInputStream(file);
                JarInputStream st = new JarInputStream(in)) {
            url = file.toURI().toURL();
            Manifest manifest = st.getManifest();
            command = manifest.getMainAttributes().getValue(MANIFEST_COMMAND);
            className = manifest.getMainAttributes().getValue(MANIFEST_CLASS);
        } catch (IOException e) {
            throw new MinionsException("Error loading minions jar: " + name, e);
        }
    }

    String getName() {
        return name;
    }

    Long getTimestamp() {
        return timestamp;
    }

    URL getURL() {
        return url;
    }

    String getCommand() {
        return command;
    }

    Minion loadMinionClass(URLClassLoader loader) {
        try {
            Class<?> clazz = Class.forName(className, true, loader);
            Class<? extends Minion> minionClazz = clazz.asSubclass(Minion.class);
            Constructor<? extends Minion> ctr = minionClazz.getConstructor();
            return ctr.newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new MinionsException("Error loading minions class: ", e);
        }
    }
}
