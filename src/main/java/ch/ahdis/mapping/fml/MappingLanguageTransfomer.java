package ch.ahdis.mapping.fml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.hl7.fhir.utilities.TextFile;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.cache.NpmPackage;
import org.hl7.fhir.utilities.cache.PackageCacheManager;
import org.hl7.fhir.utilities.cache.ToolsVersion;

public class MappingLanguageTransfomer {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MappingLanguageTransfomer.class);

	protected PackageCacheManager pcm = null;

	public MappingLanguageTransfomer() {
		try {
			pcm = new PackageCacheManager(true, ToolsVersion.TOOLS_VERSION);
		} catch (IOException e) {
			log.error("loading PackageCacheManager failed", e);
		}
	}

	// copied and adapted from ValidationEngine

	public Map<String, byte[]> loadIgSource(String src) throws Exception {
		// src can be one of the following:
		// - a canonical url for an ig - this will be converted to a package id and
		// loaded into the cache
		// - a package id for an ig - this will be loaded into the cache
		// - a direct reference to a package ("package.tgz") - this will be extracted by
		// the cache manager, but not put in the cache
		// - a folder containing resources - these will be loaded directly
		if (src.startsWith("https:") || src.startsWith("http:")) {
			String v = null;
			if (src.contains("|")) {
				v = src.substring(src.indexOf("|") + 1);
				src = src.substring(0, src.indexOf("|"));
			}
			String pid = pcm.getPackageId(src);
			if (!Utilities.noString(pid))
				return fetchByPackage(pid + (v == null ? "" : "#" + v));
		}

		File f = new File(src);
		if (f.exists()) {
			if (f.isDirectory() && new File(Utilities.path(src, "package.tgz")).exists())
				return loadPackage(new FileInputStream(Utilities.path(src, "package.tgz")), Utilities.path(src, "package.tgz"));
			if (src.endsWith(".tgz"))
				return loadPackage(new FileInputStream(src), src);
		} else if ((src.matches(PackageCacheManager.PACKAGE_REGEX)
				|| src.matches(PackageCacheManager.PACKAGE_VERSION_REGEX)) && !src.endsWith(".zip") && !src.endsWith(".tgz")) {
			return fetchByPackage(src);
		}
		throw new Exception("Unable to find/resolve/read -ig " + src);
	}

	private Map<String, byte[]> loadPackage(InputStream stream, String name) throws FileNotFoundException, IOException {
		return loadPackage(pcm.extractLocally(stream, name));
	}

	public Map<String, byte[]> loadPackage(NpmPackage pi) throws IOException {
		Map<String, byte[]> res = new HashMap<String, byte[]>();
		for (String s : pi.list("package")) {
			if (s.startsWith("CodeSystem-") || s.startsWith("ConceptMap-") || s.startsWith("ImplementationGuide-")
					|| s.startsWith("StructureMap-") || s.startsWith("ValueSet-") || s.startsWith("StructureDefinition-"))
				res.put(s, TextFile.streamToBytes(pi.load("package", s)));
		}
		String ini = "[FHIR]\r\nversion=" + pi.fhirVersion() + "\r\n";
		res.put("version.info", ini.getBytes());
		return res;
	}

	private Map<String, byte[]> fetchByPackage(String src) throws Exception {
		String id = src;
		String version = null;
		if (src.contains("#")) {
			id = src.substring(0, src.indexOf("#"));
			version = src.substring(src.indexOf("#") + 1);
		}
		NpmPackage pi = null;
		if (version == null) {
			pi = pcm.loadPackageFromCacheOnly(id);
			if (pi != null)
				log.debug("   ... Using version " + pi.version());
		} else
			pi = pcm.loadPackageFromCacheOnly(id, version);
		if (pi == null) {
			return resolvePackage(id, version);
		} else
			return loadPackage(pi);
	}

	private Map<String, byte[]> resolvePackage(String id, String v) throws Exception {
		try {
			pcm.checkBuildLoaded();
		} catch (IOException e) {
			log.error("Unable to connect to build.fhir.org to check on packages");
		}
		NpmPackage pi = pcm.loadPackage(id, v);
		if (pi != null && v == null)
			log.debug("   ... Using version " + pi.version());
		return loadPackage(pi);
	}

}
