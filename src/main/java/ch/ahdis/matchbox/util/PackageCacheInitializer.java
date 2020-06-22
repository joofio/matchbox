package ch.ahdis.matchbox.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.cache.FilesystemPackageCacheManager;
import org.hl7.fhir.utilities.cache.ToolsVersion;

public class PackageCacheInitializer {

	private FilesystemPackageCacheManager pcm = null;
	
	PackageCacheInitializer() {
		
	}
	
  private InputStream fetchFromUrlSpecific(String source) throws FHIRException {
    try {
      URL url = new URL(source);
      URLConnection c = url.openConnection();
      return c.getInputStream();
    } catch (Exception e) {
        throw new FHIRException(e.getMessage(), e);
    }
  }

	
	public void pkg(String id, String version, String tgz, String desc) throws IOException, FHIRException {
		pcm = new FilesystemPackageCacheManager(true, ToolsVersion.TOOLS_VERSION);
		if (tgz != null) {
		    InputStream inputStream=null;
		  if (Utilities.isURL(tgz)) {
		    inputStream = fetchFromUrlSpecific(tgz);
		  } else {
        inputStream = new FileInputStream(tgz);		    
		  }
			pcm.addPackageToCache(id, version, inputStream, desc);
			System.out.println("added package "+id+" version "+version);
		} else {
				pcm.loadPackage(id, version);
				System.out.println("loaded package "+id+" version "+version);
		}	
	}
	
	public static void main(String[] args) {
		
		if (hasParam(args, "-id") & hasParam(args, "-v")) {
			String id = getParam(args, "-id");
			String version = getParam(args, "-v");
			String tgz = getParam(args, "-tgz");
      String desc = getParam(args, "-desc");
			PackageCacheInitializer pci = new PackageCacheInitializer();
			try {
				pci.pkg(id, version, tgz, desc);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			} catch (FHIRException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		} else {
			System.out.println("-id package id");
			System.out.println("-v version");
			System.out.println("-tgz path to package if verison is dev");
			System.exit(-1);
		}

	}

	private static boolean hasParam(String[] args, String param) {
		for (String a : args)
			if (a.equals(param))
				return true;
		return false;
	}

	private static String getParam(String[] args, String param) {
		for (int i = 0; i < args.length - 1; i++)
			if (args[i].equals(param))
				return args[i + 1];
		return null;
	}

}
