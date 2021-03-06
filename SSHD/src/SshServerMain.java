/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */



import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.sshd.cli.server.SshServerCliSupport;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.NamedResource;
import org.apache.sshd.common.PropertyResolver;
import org.apache.sshd.common.PropertyResolverUtils;
import org.apache.sshd.common.config.ConfigFileReaderSupport;
import org.apache.sshd.common.config.SshConfigFileReader;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.config.keys.ServerIdentity;
import org.apache.sshd.server.keyprovider.AbstractGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.shell.ProcessShellCommandFactory;
import org.apache.sshd.server.shell.ShellFactory;

/**
 * TODO Add javadoc
 *
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
public class SshServerMain extends SshServerCliSupport {
    public SshServerMain() {
        super();    // in case someone wants to extend it
    }

    //////////////////////////////////////////////////////////////////////////

    public static void main(String[] args) throws Exception {
        int port = 8000;
        boolean error = false;
        String hostKeyType = AbstractGeneratorHostKeyProvider.DEFAULT_ALGORITHM;
        int hostKeySize = 0;
        Collection<String> keyFiles = null;
        Map<String, Object> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        int numArgs = GenericUtils.length(args);
        for (int i = 0; i < numArgs; i++) {
            String argName = args[i];
            if ("-p".equals(argName)) {
                i++;
                if (i >= numArgs) {
                    System.err.println("option requires an argument: " + argName);
                    error = true;
                    break;
                }
                port = Integer.parseInt(args[i]);
            } else if ("-key-type".equals(argName)) {
                i++;
                if (i >= numArgs) {
                    System.err.println("option requires an argument: " + argName);
                    error = true;
                    break;
                }

                if (keyFiles != null) {
                    System.err.println("option conflicts with -key-file: " + argName);
                    error = true;
                    break;
                }
                hostKeyType = args[i].toUpperCase();
            } else if ("-key-size".equals(argName)) {
                i++;
                if (i >= numArgs) {
                    System.err.println("option requires an argument: " + argName);
                    error = true;
                    break;
                }

                if (keyFiles != null) {
                    System.err.println("option conflicts with -key-file: " + argName);
                    error = true;
                    break;
                }

                hostKeySize = Integer.parseInt(args[i]);
            } else if ("-key-file".equals(argName)) {
                i++;
                if (i >= numArgs) {
                    System.err.println("option requires an argument: " + argName);
                    error = true;
                    break;
                }

                String keyFilePath = args[i];
                if (keyFiles == null) {
                    keyFiles = new LinkedList<>();
                }
                keyFiles.add(keyFilePath);
            } else if ("-o".equals(argName)) {
                i++;
                if (i >= numArgs) {
                    System.err.println("option requires and argument: " + argName);
                    error = true;
                    break;
                }

                String opt = args[i];
                int idx = opt.indexOf('=');
                if (idx <= 0) {
                    System.err.println("bad syntax for option: " + opt);
                    error = true;
                    break;
                }

                String optName = opt.substring(0, idx);
                String optValue = opt.substring(idx + 1);
                if (ServerIdentity.HOST_KEY_CONFIG_PROP.equals(optName)) {
                    if (keyFiles == null) {
                        keyFiles = new LinkedList<>();
                    }
                    keyFiles.add(optValue);
                } else if (ConfigFileReaderSupport.PORT_CONFIG_PROP.equals(optName)) {
                    port = Integer.parseInt(optValue);
                } else {
                    options.put(optName, optValue);
                }
            }
        }

        SshServer sshd = error ? null : setupIoServiceFactory(SshServer.setUpDefaultServer(), options, System.out, System.err, args);
        if (sshd == null) {
            error = true;
        }

        if (error) {
            System.err.println("usage: sshd [-p port] [-io mina|nio2|netty] [-key-type RSA|DSA|EC] [-key-size NNNN] [-key-file <path>] [-o option=value]");
            System.exit(-1);
        }

        Map<String, Object> props = sshd.getProperties();
        props.putAll(options);

        PropertyResolver resolver = PropertyResolverUtils.toPropertyResolver(options);
        KeyPairProvider hostKeyProvider = resolveServerKeys(System.err, hostKeyType, hostKeySize, keyFiles);
        sshd.setKeyPairProvider(hostKeyProvider);
        // Should come AFTER key pair provider setup so auto-welcome can be generated if needed
        setupServerBanner(sshd, resolver);
        sshd.setPort(port);

        String macsOverride = resolver.getString(ConfigFileReaderSupport.MACS_CONFIG_PROP);
        if (GenericUtils.isNotEmpty(macsOverride)) {
            SshConfigFileReader.configureMacs(sshd, macsOverride, true, true);
        }

        ShellFactory shellFactory = resolveShellFactory(System.err, resolver);
        if (shellFactory != null) {
            System.out.append("Using shell=").println(shellFactory.getClass().getName());
            sshd.setShellFactory(shellFactory);
        }
      //set user name and password here
        sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
      	  @Override
      	  public boolean authenticate(String username, String password, ServerSession session) {
      	    if ((username.equals("ubuntu")) && (password.equals("dataview"))) {
      	     // sshd.setFileSystemFactory(new VirtualFileSystemFactory(new File("C:\\devl").toPath()));
      	      return true;
      	    }
      	    return false;
      	  }
      	});
        
        //sshd.setPasswordAuthenticator((username, password, session) -> Objects.equals(username, password));
      //  sshd.setPublickeyAuthenticator(AcceptAllPublickeyAuthenticator.INSTANCE);
     //   sshd.setPasswordAuthenticator((username, password, session) -> Objects.equals(username, password));
       // sshd.setPublickeyAuthenticator(AcceptAllPublickeyAuthenticator.INSTANCE);
        setupServerForwarding(sshd, resolver);
        sshd.setCommandFactory(new ScpCommandFactory.Builder()
            .withDelegate(ProcessShellCommandFactory.INSTANCE)
            .build());

        List<NamedFactory<Command>> subsystems = resolveServerSubsystems(System.err, resolver);
        if (GenericUtils.isNotEmpty(subsystems)) {
            System.out.append("Setup subsystems=").println(NamedResource.getNames(subsystems));
            sshd.setSubsystemFactories(subsystems);
        }

        System.err.println("Starting SSHD on port " + port);
        sshd.start();
        //Thread.sleep(Long.MAX_VALUE);
        while(true) {
        	File file1 = new File("/home/ubuntu/CodeProvisioner.jar");
        	File file2 = new File("/home/ubuntu/codeprovisioner.jks");
        	File file3 = new File("/home/ubuntu/TaskExecutor.enc");

        	if (file1.exists() && file2.exists() && file3.exists()) {
        	break;
        	}
        	System.out.println("Waiting for 30 secods to retrieve all necessary files.");
        	Thread.sleep(30000);
        	}
        
        System.out.println("Starting CodeProvisioner by java reflection");
        sshd.close();
        URLClassLoader child = new URLClassLoader (new URL[] {new URL("file:///home/ubuntu/CodeProvisioner.jar")}, SshServerMain.class.getClassLoader());
		Class classToLoad = Class.forName("dataview.workflowexecutor.CodeProvisioner", true, child);
		Method method = classToLoad.getDeclaredMethod("run1");
		//String[] point= new String[0];
		//Method method = classToLoad.getDeclaredMethod("main",String[].class);
		//method.invoke(null, (Object)point);
		Object instance = classToLoad.newInstance();
		Object result = method.invoke(instance);
		
        
    }
}