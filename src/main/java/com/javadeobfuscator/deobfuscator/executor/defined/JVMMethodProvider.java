/*
 * Copyright 2016 Sam Sun <me@samczsun.com>
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.javadeobfuscator.deobfuscator.executor.defined;

import java.io.*;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.javadeobfuscator.deobfuscator.executor.ThreadStore;
import com.javadeobfuscator.deobfuscator.executor.defined.types.*;
import com.javadeobfuscator.deobfuscator.executor.exceptions.ExecutionException;
import com.javadeobfuscator.deobfuscator.executor.values.*; 
import com.javadeobfuscator.deobfuscator.org.objectweb.asm.tree.ClassNode; 
import com.javadeobfuscator.deobfuscator.org.objectweb.asm.tree.FieldNode; 
import com.javadeobfuscator.deobfuscator.executor.values.JavaCharacter;
import com.javadeobfuscator.deobfuscator.executor.values.JavaInteger;
import com.javadeobfuscator.deobfuscator.executor.values.JavaValue;
import com.javadeobfuscator.deobfuscator.utils.Utils;

import com.javadeobfuscator.deobfuscator.executor.Context;
import com.javadeobfuscator.deobfuscator.executor.providers.MethodProvider;
import com.javadeobfuscator.deobfuscator.org.objectweb.asm.Type;
import com.javadeobfuscator.deobfuscator.utils.WrappedClassNode; 

import javax.xml.bind.DatatypeConverter;

import javax.xml.bind.DatatypeConverter;

public class JVMMethodProvider extends MethodProvider {
    @SuppressWarnings("serial")
    //@formatter:off
    private static final Map<String, Map<String, Function3<JavaValue, List<JavaValue>, Context, Object>>> functions = new HashMap<String, Map<String, Function3<JavaValue, List<JavaValue>, Context, Object>>>() {{
        // Java
        put("java/lang/Object", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("getClass()Ljava/lang/Class;", (targetObject, args, context) -> new JavaClass(Type.getType(targetObject.value().getClass()).getInternalName(), context));
            put("notifyAll()V", (targetObject, args, context) -> {
                synchronized (targetObject.as(Object.class)) {
                    targetObject.as(Object.class).notifyAll();
                }
                return null;
            });
            put("wait(J)V", (targetObject, args, context) -> {
                synchronized (targetObject.as(Object.class)) {
                    targetObject.as(Object.class).wait((long) args.get(0).value());
                }
                return null;
            });
            put("<init>()V", (targetObject, args, context) -> {
                expect(targetObject, targetObject.type()); 
                targetObject.initialize(new JavaObject(null, targetObject.type())); 
                initObject(context, targetObject.type(), targetObject); 
                return null;
            });
        }});
        put("java/util/zip/ZipInputStream", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("<init>(Ljava/io/InputStream;)V", (targetObject, args, context) -> {
                System.out.println("New ZipInputStream with " + args.get(0).value());
                targetObject.initialize(new ZipInputStream(args.get(0).as(InputStream.class)));
                return null;
            });
        }});
        put("java/nio/charset/Charset", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("availableCharsets()Ljava/util/SortedMap;", (targetObject, args, context) -> Charset.availableCharsets());
        }});
        put("java/util/SortedMap", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("keySet()Ljava/util/Set;", (targetObject, args, context) -> targetObject.as(SortedMap.class).keySet());
        }});
        put("java/util/Set", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("iterator()Ljava/util/Iterator;", (targetObject, args, context) -> targetObject.as(Set.class).iterator());
        }});
        put("java/util/Iterator", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("hasNext()Z", (targetObject, args, context) -> targetObject.as(Iterator.class).hasNext());
            put("next()Ljava/lang/Object;", (targetObject, args, context) -> targetObject.as(Iterator.class).next());
        }});
        put("java/io/ByteArrayOutputStream", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("<init>()V", (targetObject, args, context) -> {
                targetObject.initialize(new ByteArrayOutputStream());
                return null;
            });
            put("close()V", (targetObject, args, context) -> {
                try {
                    targetObject.as(ByteArrayOutputStream.class).close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            });
            put("toByteArray()[B", (targetObject, args, context) -> targetObject.as(ByteArrayOutputStream.class).toByteArray());
            put("write([B)V", (targetObject, args, context) -> {
                try {
                    targetObject.as(ByteArrayOutputStream.class).write(args.get(0).as(byte[].class));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            });
        }});
        put("java/util/Arrays", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("asList([Ljava/lang/Object;)Ljava/util/List;", (targetObject, args, context) -> Arrays.asList(args.get(0).as(Object[].class)));
        }});
        put("java/lang/String", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("<init>([CII)V", (targetObject, args, context) -> {
                expect(targetObject, "java/lang/String");
                targetObject.initialize(new String(args.get(0).as(char[].class), args.get(1).intValue(), args.get(2).intValue()));
                return null;
            });
            put("<init>([C)V", (targetObject, args, context) -> {
                expect(targetObject, "java/lang/String");
                targetObject.initialize(new String(args.get(0).as(char[].class)));
                return null;
            });
            put("<init>([B)V", (targetObject, args, context) -> {
                expect(targetObject, "java/lang/String");
                targetObject.initialize(new String(args.get(0).as(byte[].class)));
                return null;
            });
            put("<init>([BI)V", (targetObject, args, context) -> {
                expect(targetObject, "java/lang/String");
                targetObject.initialize(new String(args.get(0).as(byte[].class), args.get(1).intValue()));
                return null;
            });
            put("intern()Ljava/lang/String;", (targetObject, args, context) -> targetObject.as(String.class).intern());
            put("equals(Ljava/lang/Object;)Z", (targetObject, args, context) -> targetObject.as(String.class).equals(args.get(0).value()));
            put("trim()Ljava/lang/String;", (targetObject, args, context) -> targetObject.as(String.class).trim());
            put("toCharArray()[C", (targetObject, args, context) -> targetObject.as(String.class).toCharArray());
            put("length()I", (targetObject, args, context) -> targetObject.as(String.class).length());
            put("hashCode()I", (targetObject, args, context) -> targetObject.as(String.class).hashCode());
            put("charAt(I)C", (targetObject, args, context) -> targetObject.as(String.class).charAt(args.get(0).intValue()));
            put("indexOf(I)I", (targetObject, args, context) -> targetObject.as(String.class).indexOf(args.get(0).intValue()));
            put("endsWith(Ljava/lang/String;)Z", (targetObject, args, context) -> targetObject.as(String.class).endsWith(args.get(0).as(String.class)));
            put("startsWith(Ljava/lang/String;)Z", (targetObject, args, context) -> targetObject.as(String.class).startsWith(args.get(0).as(String.class)));
            put("substring(I)Ljava/lang/String;", (targetObject, args, context) -> targetObject.as(String.class).substring(args.get(0).intValue()));
            put("substring(II)Ljava/lang/String;", (targetObject, args, context) -> targetObject.as(String.class).substring(args.get(0).intValue(), args.get(1).intValue()));
            put("indexOf(II)I", (targetObject, args, context) -> targetObject.as(String.class).indexOf(args.get(0).intValue(), args.get(1).intValue()));
            put("lastIndexOf(I)I", (targetObject, args, context) -> targetObject.as(String.class).lastIndexOf(args.get(0).intValue()));
            put("split(Ljava/lang/String;)[Ljava/lang/String;", (targetObject, args, context) -> targetObject.as(String.class).split(args.get(0).as(String.class)));
            put("valueOf(Ljava/lang/Object;)Ljava/lang/String;", (targetObject, args, context) -> String.valueOf(args.get(0).value()));
            put("getBytes(Ljava/lang/String;)[B", (targetObject, args, context) -> {
                try {
                    return targetObject.as(String.class).getBytes(args.get(0).as(String.class));
                } catch (UnsupportedEncodingException e) {
                    throw new ExecutionException(e);
                }
            });
            put("valueOf([CII)Ljava/lang/String;", (targetObject, args, context) -> String.valueOf(args.get(0).as(char[].class), args.get(1).intValue(), args.get(2).intValue()));
        }});
        put("java/lang/StringBuilder", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("<init>()V", (targetObject, args, context) -> {
                expect(targetObject, "java/lang/StringBuilder");
                targetObject.initialize(new StringBuilder());
                return null;
            });
            put("<init>(Ljava/lang/String;)V", (targetObject, args, context) -> {
                expect(targetObject, "java/lang/StringBuilder");
                targetObject.initialize(new StringBuilder(args.get(0).as(String.class)));
                return null;
            });
            put("append(I)Ljava/lang/StringBuilder;", (targetObject, args, context) -> targetObject.as(StringBuilder.class).append(args.get(0).intValue()));
            put("append(C)Ljava/lang/StringBuilder;", (targetObject, args, context) -> targetObject.as(StringBuilder.class).append(args.get(0).as(char.class)));
            put("append(Ljava/lang/String;)Ljava/lang/StringBuilder;", (targetObject, args, context) -> targetObject.as(StringBuilder.class).append(args.get(0).as(String.class)));
            put("append(Ljava/lang/Object;)Ljava/lang/StringBuilder;", (targetObject, args, context) -> targetObject.as(StringBuilder.class).append(args.get(0).as(Object.class)));
            put("toString()Ljava/lang/String;", (targetObject, args, context) -> targetObject.as(StringBuilder.class).toString());
            put("length()I", (targetObject, args, context) -> targetObject.as(StringBuilder.class).length());
            put("charAt(I)C", (targetObject, args, context) -> targetObject.as(StringBuilder.class).charAt(args.get(0).intValue()));
            put("setCharAt(IC)V", (targetObject, args, context) -> {
                targetObject.as(StringBuilder.class).setCharAt(args.get(0).intValue(), (char) ((JavaCharacter) args.get(1)).charValue());
                return null;
            });
        }});
        put("java/lang/StringBuffer", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("<init>(Ljava/lang/String;)V", (targetObject, args, context) -> {
                expect(targetObject, "java/lang/StringBuffer");
                targetObject.initialize(new StringBuffer(args.get(0).as(String.class)));
                return null;
            });
            put("<init>(I)V", (targetObject, args, context) -> {
                expect(targetObject, "java/lang/StringBuffer");
                targetObject.initialize(new StringBuffer(args.get(0).intValue()));
                return null;
            });
            put("<init>()V", (targetObject, args, context) -> {
                expect(targetObject, "java/lang/StringBuffer");
                targetObject.initialize(new StringBuffer());
                return null;
            });
            put("insert(ILjava/lang/String;)Ljava/lang/StringBuffer;", (targetObject, args, context) -> targetObject.as(StringBuffer.class).insert(args.get(0).intValue(), args.get(1).as(String.class)));
            put("append(Ljava/lang/String;)Ljava/lang/StringBuffer;", (targetObject, args, context) -> targetObject.as(StringBuffer.class).append(args.get(0).as(String.class)));
            put("append(C)Ljava/lang/StringBuffer;", (targetObject, args, context) -> targetObject.as(StringBuffer.class).append(args.get(0).as(char.class)));
            put("toString()Ljava/lang/String;", (targetObject, args, context) -> targetObject.as(StringBuffer.class).toString());
        }});
        put("java/lang/Exception", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("<init>()V", (targetObject, args, context) -> {
                expect(targetObject, "java/lang/Exception");
                targetObject.initialize(null);
                return null;
            });
            put("getStackTrace()[Ljava/lang/StackTraceElement;", (targetObject, args, context) -> context.getStackTrace());
            put("toString()Ljava/lang/String;", (targetObject, args, context) -> targetObject.toString());
        }});
        put("java/lang/Throwable", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("<init>()V", (targetObject, args, context) -> {
                expect(targetObject, "java/lang/Throwable");
                targetObject.initialize(null);
                return null;
            });
            put("getStackTrace()[Ljava/lang/StackTraceElement;", (targetObject, args, context) -> context.getStackTrace());
            put("toString()Ljava/lang/String;", (targetObject, args, context) -> targetObject.toString());
        }});
        put("java/lang/NullPointerException", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("<init>()V", (targetObject, args, context) -> {
                expect(targetObject, "java/lang/NullPointerException");
                targetObject.initialize(null);
                return null;
            });
            put("getStackTrace()[Ljava/lang/StackTraceElement;", (targetObject, args, context) -> context.getStackTrace());
            put("toString()Ljava/lang/String;", (targetObject, args, context) -> targetObject.toString());
        }});
        put("java/lang/RuntimeException", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("<init>(Ljava/lang/String;)V", (targetObject, args, context) -> {
                expect(targetObject, "java/lang/RuntimeException");
                targetObject.initialize(args.get(0).value());
                return null;
            });
            put("<init>()V", (targetObject, args, context) -> {
                expect(targetObject, "java/lang/RuntimeException");
                targetObject.initialize(new RuntimeException());
                return null;
            });
            put("getStackTrace()[Ljava/lang/StackTraceElement;", (targetObject, args, context) -> context.getStackTrace());
        }});
        put("java/lang/Class", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("forName(Ljava/lang/String;)Ljava/lang/Class;", (targetObject, args, context) -> new JavaClass(args.get(0).as(String.class), context));
            put("getDeclaredMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", (targetObject, args, context) -> targetObject.as(JavaClass.class).getDeclaredMethod(args.get(0).as(String.class), toJavaClass(args.get(1).as(Object[].class))));
            put("getDeclaredMethods()[Ljava/lang/reflect/Method;", (targetObject, args, context) -> targetObject.as(JavaClass.class).getDeclaredMethods());
            put("getMethods()[Ljava/lang/reflect/Method;", (targetObject, args, context) -> targetObject.as(JavaClass.class).getDeclaredMethods());//FIXME: Temporary patch
            put("getDeclaredField(Ljava/lang/String;)Ljava/lang/reflect/Field;", (targetObject, args, context) -> targetObject.as(JavaClass.class).getDeclaredField(args.get(0).as(String.class)));
            put("getDeclaredFields()[Ljava/lang/reflect/Field;", (targetObject, args, context) -> targetObject.as(JavaClass.class).getDeclaredFields());
            put("getClassLoader()Ljava/lang/ClassLoader;", (targetObject, args, context) -> null);
            put("getName()Ljava/lang/String;", (targetObject, args, context) -> targetObject.as(JavaClass.class).getName());
            put("getSimpleName()Ljava/lang/String;", (targetObject, args, context) -> targetObject.as(JavaClass.class).getSimpleName());
            put("getSuperclass()Ljava/lang/Class;", (targetObject, args, context) -> targetObject.as(JavaClass.class).getSuperclass());
            put("getInterfaces()[Ljava/lang/Class;", (targetObject, args, context) -> targetObject.as(JavaClass.class).getInterfaces());
            put("getProtectionDomain()Ljava/security/ProtectionDomain;", (targetObject, args, context) -> new ProtectionDomain(new CodeSource(context.file.toURI().toURL(), new Certificate[0]), null));
            put("isInterface()Z", (targetObject, args, context) -> targetObject.as(JavaClass.class).isInterface());
        }});
        put("java/security/ProtectionDomain", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("getCodeSource()Ljava/security/CodeSource;", (targetObject, args, context) -> targetObject.as(ProtectionDomain.class).getCodeSource());
        }});
        put("java/security/CodeSource", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("getLocation()Ljava/net/URL;", (targetObject, args, context) -> targetObject.as(CodeSource.class).getLocation());
        }});
        put("java/net/URL", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            // Probably not an issue because you can't construct URLs yet
            put("openStream()Ljava/io/InputStream;", (targetObject, args, context) -> {
                URL url = targetObject.as(URL.class);
                if (url.getProtocol().equals("file")) {
                    return url.openStream();
                }
                throw new ExecutionException("Disallowed opening URL for now");
            });
        }});
        put("java/util/zip/ZipInputStream", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("<init>(Ljava/io/InputStream;)V", (targetObject, args, context) -> {
                expect(targetObject, "java/util/zip/ZipInputStream");
                targetObject.initialize(new ZipInputStream(args.get(0).as(InputStream.class)));
                return null;
            });
            put("getNextEntry()Ljava/util/zip/ZipEntry;", (targetObject, args, context) -> targetObject.as(ZipInputStream.class).getNextEntry());
            put("closeEntry()V", (targetObject, args, context) -> {
                targetObject.as(ZipInputStream.class).closeEntry();
                return null;
            });
        }});
        put("java/util/zip/ZipEntry", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("getName()Ljava/lang/String;", (targetObject, args, context) -> targetObject.as(ZipEntry.class).getName());
            put("getExtra()[B", (targetObject, args, context) -> targetObject.as(ZipEntry.class).getExtra());
        }});
        put("java/lang/reflect/Constructor", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("getParameterTypes()[Ljava/lang/Class;", (targetObject, args, context) -> targetObject.as(JavaConstructor.class).getParameterTypes());
            put("setAccessible(Z)V", (targetObject, args, context) -> {
                targetObject.as(JavaConstructor.class).setAccessible(args.get(0).as(boolean.class));
                return null;
            });

            put("newInstance([Ljava/lang/Object;)Ljava/lang/Object;", (targetObject, args, context) -> targetObject.as(JavaConstructor.class)); // FIXME
        }});
        put("java/lang/reflect/Method", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("getName()Ljava/lang/String;", (targetObject, args, context) -> targetObject.as(JavaMethod.class).getName());
            put("getReturnType()Ljava/lang/Class;", (targetObject, args, context) -> targetObject.as(JavaMethod.class).getReturnType());
            put("getParameterTypes()[Ljava/lang/Class;", (targetObject, args, context) -> targetObject.as(JavaMethod.class).getParameterTypes());
            put("setAccessible(Z)V", (targetObject, args, context) -> {
                targetObject.as(JavaMethod.class).setAccessible(args.get(0).as(boolean.class));
                return null;
            });
            put("hashCode()I", (targetObject, args, context) -> targetObject.as(JavaMethod.class).hashCode());
            put("invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", (targetObject, args, context) -> targetObject.as(JavaMethod.class).invoke(args.get(0), args.get(1).as(Object[].class)));
        }});
        put("java/lang/reflect/Field", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("getName()Ljava/lang/String;", (targetObject, args, context) -> targetObject.as(JavaField.class).getName());
            put("getType()Ljava/lang/Class;", (targetObject, args, context) -> targetObject.as(JavaField.class).getType());
            put("setAccessible(Z)V", (targetObject, args, context) -> {
                targetObject.as(JavaField.class).setAccessible(args.get(0).as(boolean.class));
                return null;
            });
            put("getModifiers()I", (targetObject, args, context) -> targetObject.as(JavaField.class).getModifiers());
            put("setInt(Ljava/lang/Object;I)V", (targetObject, args, context) -> {
                targetObject.as(JavaField.class).setInt(args.get(0).as(Object.class), args.get(1).intValue());
                return null;
            });
            put("set(Ljava/lang/Object;Ljava/lang/Object;)V", (targetObject, args, context) -> {
                targetObject.as(JavaField.class).set(args.get(0).as(Object.class), args.get(0).as(Object.class));
                return null;
            });
            put("get(Ljava/lang/Object;)Ljava/lang/Object;", (targetObject, args, context) -> targetObject.as(JavaField.class).get(args.get(0).as(Object.class)));
        }});
        put("java/lang/reflect/Modifier", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("isStatic(I)Z", (targetObject, args, context) -> Modifier.isStatic(args.get(0).intValue()));
            put("isFinal(I)Z", (targetObject, args, context) -> Modifier.isFinal(args.get(0).intValue()));
        }});
        put("java/lang/invoke/MethodType", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("fromMethodDescriptorString(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;", (targetObject, args, context) -> args.get(0).value());
        }});
        put("java/lang/invoke/MethodHandles$Lookup", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("findStatic(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", (targetObject, args, context) -> new JavaMethodHandle(args.get(0).as(JavaClass.class).getType().getInternalName(), args.get(1).as(String.class), args.get(2).as(String.class), "static"));
            put("findVirtual(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", (targetObject, args, context) -> new JavaMethodHandle(args.get(0).as(JavaClass.class).getType().getInternalName(), args.get(1).as(String.class), args.get(2).as(String.class), "virtual"));
            put("unreflect(Ljava/lang/reflect/Method;)Ljava/lang/invoke/MethodHandle;", (targetObject, args, context) -> new JavaMethodHandle(args.get(0).as(JavaMethod.class).getDeclaringClass().getName().replace(".", "/"), args.get(0).as(JavaMethod.class).getName(), args.get(0).as(JavaMethod.class).getDesc(), args.get(0).as(JavaMethod.class).isStatic() ? "static" : "virtual"));
        }});
        put("java/lang/invoke/MethodHandle", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("asType(Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", (targetObject, args, context) -> targetObject.value());
        }});
        put("java/lang/invoke/ConstantCallSite", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("<init>(Ljava/lang/invoke/MethodHandle;)V", (targetObject, args, context) -> {
                expect(targetObject, "java/lang/invoke/ConstantCallSite");
                targetObject.initialize(args.get(0).value());
                return null;
            });
        }});
        put("java/lang/System", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("currentTimeMillis()J", (targetObject, args, context) -> System.currentTimeMillis());
            put("arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V", (targetObject, args, context) -> {
                System.arraycopy(args.get(0).value(), args.get(1).intValue(), args.get(2).value(), args.get(3).intValue(), args.get(4).intValue());
                return null;
            });
        }});
        put("java/lang/Thread", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("start()V", (targetObject, args, context) -> {
                targetObject.as(JavaThread.class).start();
                return null;
            });
            put("currentThread()Ljava/lang/Thread;", (targetObject, args, context) -> ThreadStore.retrieveThread(Thread.currentThread().getId()));
            put("getStackTrace()[Ljava/lang/StackTraceElement;", (targetObject, args, context) -> {
                context.push("java.lang.Thread", "getStackTrace", 0);
                StackTraceElement[] elems = context.getStackTrace();
                context.pop();
                return elems;
            });
            put("join()V", (targetObject, args, context) -> {
                targetObject.as(JavaThread.class).getThread().join();
                return null;
            });
            put("yield()V", (targetObject, args, context) -> {
                Thread.yield();
                return null;
            });
            put("<init>()V", (targetObject, args, context) -> {
                targetObject.initialize(new JavaThread(context, (JavaObject) targetObject));
                return null;
            });
        }});
        put("java/lang/StackTraceElement", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("getClassName()Ljava/lang/String;", (targetObject, args, context) -> targetObject.as(StackTraceElement.class).getClassName());
            put("getMethodName()Ljava/lang/String;", (targetObject, args, context) -> targetObject.as(StackTraceElement.class).getMethodName());
        }});
        put("java/lang/Long", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("parseLong(Ljava/lang/String;)J", (targetObject, args, context) -> Long.parseLong(args.get(0).as(String.class)));
            put("parseLong(Ljava/lang/String;I)J", (targetObject, args, context) -> Long.parseLong(args.get(0).as(String.class), args.get(1).intValue()));
            put("valueOf(J)Ljava/lang/Long;", (targetObject, args, context) -> Long.valueOf(args.get(0).longValue()));
        }});
        put("java/lang/Integer", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("parseInt(Ljava/lang/String;)I", (targetObject, args, context) -> Integer.parseInt(args.get(0).as(String.class)));
            put("valueOf(Ljava/lang/String;)Ljava/lang/Integer;", (targetObject, args, context) -> new JavaInteger(Integer.valueOf(args.get(0).as(String.class))));
            put("valueOf(Ljava/lang/String;I)Ljava/lang/Integer;", (targetObject, args, context) -> new JavaInteger(Integer.valueOf(args.get(0).as(String.class), args.get(1).intValue())));
            put("valueOf(I)Ljava/lang/Integer;", (targetObject, args, context) -> args.get(0));
            put("intValue()I", (targetObject, args, context) -> ((JavaInteger) targetObject.value()).value());
        }});
        put("java/util/regex/Pattern", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("compile(Ljava/lang/String;)Ljava/util/regex/Pattern;", (targetObject, args, context) -> Pattern.compile(args.get(0).as(String.class)));
        }});
        put("java/lang/BootstrapMethodError", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("<init>()V", (targetObject, args, context) -> {
                expect(targetObject, "java/lang/BootstrapMethodError");
                targetObject.initialize(new BootstrapMethodError());
                return null;
            });
        }});
        put("java/util/TreeMap", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("<init>()V", (targetObject, args, context) -> {
                expect(targetObject, "java/util/TreeMap");
                targetObject.initialize(new TreeMap<>());
                return null;
            });
        }});
        put("java/util/HashMap", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("<init>()V", (targetObject, args, context) -> {
                expect(targetObject, "java/util/HashMap");
                targetObject.initialize(new HashMap<>());
                return null;
            });
            put("<init>(I)V", (targetObject, args, context) -> {
                expect(targetObject, "java/util/HashMap");
                targetObject.initialize(new HashMap<>(args.get(0).as(Integer.class)));
                return null;
            });
            put("put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", (targetObject, args, context) -> targetObject.as(HashMap.class).put(args.get(0), args.get(1)));
        }});
        put("java/util/LinkedList", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("<init>()V", (targetObject, args, context) -> {
                expect(targetObject, "java/util/LinkedList");
                targetObject.initialize(new LinkedList<>());
                return null;
            });
        }});
        put("java/util/LinkedHashMap", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{ 
            put("<init>()V", (targetObject, args, context) -> { 
                expect(targetObject, "java/util/LinkedHashMap"); 
                targetObject.initialize(new LinkedHashMap<>()); 
                return null; 
            }); 
            put("<init>(Ljava/util/Map;)V", (targetObject, args, context) -> { 
                expect(targetObject, "java/util/LinkedHashMap"); 
                targetObject.initialize(new LinkedHashMap<>(args.get(0).as(Map.class))); 
                return null; 
            }); 
            put("<init>(IFZ)V", (targetObject, args, context) -> { 
                expect(targetObject, "java/util/LinkedHashMap"); 
                JavaValue accessOrder = args.get(2); 
                targetObject.initialize(new LinkedHashMap<>(args.get(0).intValue(), args.get(1).floatValue(), accessOrder instanceof JavaBoolean ? accessOrder.booleanValue() : accessOrder.intValue() == 1)); 
                return null; 
            }); 
            put("get(Ljava/lang/Object;)Ljava/lang/Object;", (targetObject, args, context) -> targetObject.as(LinkedHashMap.class).get(args.get(0).value())); 
            put("put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", (targetObject, args, context) -> targetObject.as(LinkedHashMap.class).put(args.get(0).value(), args.get(1).value())); 
            put("isEmpty()Z", (targetObject, args, context) -> targetObject.as(LinkedHashMap.class).isEmpty()); 
            put("entrySet()Ljava/util/Set;", (targetObject, args, context) -> targetObject.as(LinkedHashMap.class).entrySet()); 
            put("remove(Ljava/lang/Object;)Ljava/lang/Object;", (targetObject, args, context) -> targetObject.as(LinkedHashMap.class).remove(args.get(0).value())); 
        }}); 
        put("java/lang/Math", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("abs(J)J", (targetObject, args, context) -> Math.abs(args.get(0).longValue()));
        }});
        put("java/math/BigInteger", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("<init>(Ljava/lang/String;I)V", (targetObject, args, context) -> {
                expect(targetObject, "java/math/BigInteger");
                if (args.get(1).value() instanceof Byte) { // FIXME
                    targetObject.initialize(new BigInteger(args.get(0).as(String.class), (byte) args.get(1).value()));
                } else if (args.get(1).value() instanceof Short) {
                    targetObject.initialize(new BigInteger(args.get(0).as(String.class), (short) args.get(1).value()));
                } else {
                    targetObject.initialize(new BigInteger(args.get(0).as(String.class), (int) args.get(1).value()));
                }
                return null;
            });
            put("add(Ljava/math/BigInteger;)Ljava/math/BigInteger;", (targetObject, args, context) -> targetObject.as(BigInteger.class).add(args.get(0).as(BigInteger.class)));
            put("xor(Ljava/math/BigInteger;)Ljava/math/BigInteger;", (targetObject, args, context) -> targetObject.as(BigInteger.class).xor(args.get(0).as(BigInteger.class)));
            put("modPow(Ljava/math/BigInteger;Ljava/math/BigInteger;)Ljava/math/BigInteger;", (targetObject, args, context) -> targetObject.as(BigInteger.class).modPow(args.get(0).as(BigInteger.class), args.get(1).as(BigInteger.class)));
            put("intValue()I", (targetObject, args, context) -> new JavaInteger(targetObject.as(BigInteger.class).intValue()).intValue());
        }});

        // Javax
        put("javax/xml/bind/DatatypeConverter", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("parseBase64Binary(Ljava/lang/String;)[B", (targetObject, args, context) -> DatatypeConverter.parseBase64Binary(args.get(0).as(String.class)));
        }});

        // Sun
        put("sun/misc/SharedSecrets", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("getJavaLangAccess()Lsun/misc/JavaLangAccess;", (targetObject, args, context) -> null);
        }});
        put("sun/misc/JavaLangAccess", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("getConstantPool(Ljava/lang/Class;)Lsun/reflect/ConstantPool;", (targetObject, args, context) -> new JavaConstantPool(args.get(0).as(JavaClass.class)));
        }});
        put("sun/reflect/ConstantPool", new HashMap<String, Function3<JavaValue, List<JavaValue>, Context, Object>>() {{
            put("getSize()I", (targetObject, args, context) -> targetObject.as(JavaConstantPool.class).getSize());
        }});

    }};
    //@formatter:on

    @Override
    public boolean instanceOf(JavaValue target, Type type, Context context) {
        return false;
    }

    @Override
    public Object invokeMethod(String className, String methodName, String methodDesc, JavaValue targetObject, List<JavaValue> args, Context context) {
        Map<String, Function3<JavaValue, List<JavaValue>, Context, Object>> map = functions.get(className);
        return map.get(methodName + methodDesc).applyUnchecked(targetObject, args, context);
    }

    @Override
    public boolean canInvokeMethod(String className, String methodName, String methodDesc, JavaValue targetObject, List<JavaValue> args, Context context) {
        Map<String, Function3<JavaValue, List<JavaValue>, Context, Object>> map = functions.get(className);
        return map != null && map.containsKey(methodName + methodDesc);
    }

    @Override
    public boolean canCheckInstanceOf(JavaValue target, Type type, Context context) {
        return false;
    }

    private static void initObject(Context context, String className, JavaValue object) { 
        WrappedClassNode wrappedClassNode = context.dictionary.get(className); 
        if (wrappedClassNode != null) { 
            ClassNode classNode = wrappedClassNode.classNode; 
 
            for (FieldNode field : classNode.fields) { 
                switch (field.desc) { 
                    case "B": 
                        context.provider.setField(classNode.name, field.name, field.desc, object, (byte) 0, context); 
                        break; 
                    case "S": 
                        context.provider.setField(classNode.name, field.name, field.desc, object, (short) 0, context); 
                        break; 
                    case "I": 
                        context.provider.setField(classNode.name, field.name, field.desc, object, 0, context); 
                        break; 
                    case "J": 
                        context.provider.setField(classNode.name, field.name, field.desc, object, 0L, context); 
                        break; 
                    case "F": 
                        context.provider.setField(classNode.name, field.name, field.desc, object, 0.0, context); 
                        break; 
                    case "D": 
                        context.provider.setField(classNode.name, field.name, field.desc, object, 0.0D, context); 
                        break; 
                    case "C": 
                        context.provider.setField(classNode.name, field.name, field.desc, object, (char) 0, context); 
                        break; 
                    case "Z": 
                        context.provider.setField(classNode.name, field.name, field.desc, object, false, context); 
                        break; 
                } 
            } 
        } else { 
            throw new RuntimeException("Could not initialize class " + className); 
        } 
    } 
 

    private static void expect(JavaValue object, String type) {
        if (!object.type().equals(type)) {
            throw new IllegalArgumentException("Expected UninitializedObject[" + type + "] but got " + object.type());
        }
    }

    @Override
    public boolean checkEquality(JavaValue first, JavaValue second, Context context) {
        if (first.value() instanceof JavaClass && second.value() instanceof JavaClass) {
            return first.as(JavaClass.class).equals(second.value());
        }
        return first == second;
    }

    @Override
    public boolean canCheckEquality(JavaValue first, JavaValue second, Context context) {
        return true;
    }

    private static JavaClass[] toJavaClass(Object[] arr) {
        JavaClass[] clazz = new JavaClass[arr.length];
        for (int i = 0; i < arr.length; i++) {
            clazz[i] = (JavaClass) arr[i];
        }
        return clazz;
    }

    @FunctionalInterface
    public interface Function3<T1, T2, T3, R> {

        default R applyUnchecked(T1 var1, T2 var2, T3 var3) {
            try {
                return this.apply(var1, var2, var3);
            } catch (Throwable t) {
                Utils.sneakyThrow(t);
            }
            return null;
        }

        R apply(T1 var1, T2 var2, T3 var3) throws Throwable;
    }
}
