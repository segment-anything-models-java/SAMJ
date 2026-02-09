package ai.nets.samj.install;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import io.bioimage.modelrunner.apposed.appose.Mamba;
import io.bioimage.modelrunner.apposed.appose.MambaInstallException;
import io.bioimage.modelrunner.system.PlatformDetection;

public class DependencyChecker {
	
	private DependencyChecker() {}

	
	/**
	 * Check whether a list of dependencies provided is installed in the wanted environment.
	 * 
	 * @param envName
	 * 	The name of the environment of interest. Should be one of the environments of the current Mamba instance.
	 * 	This parameter can also be the full path to an independent environment.
	 * @param dependencies
	 * 	The list of dependencies that should be installed in the environment.
	 * 	They can contain version requirements. The names should be the ones used to import the package inside python,
	 * 	"skimage", not "scikit-image" or "sklearn", not "scikit-learn"
	 * 	An example list: "numpy", "numba&gt;=0.43.1", "torch==1.6", "torch&gt;=1.6, &lt;2.0"
	 * @return true if the packages are installed or false otherwise
	 */
	public static boolean checkAllDependenciesInEnv(String envName, List<String> dependencies) throws MambaInstallException {
		return checkUninstalledDependenciesInEnv(envName, dependencies).size() == 0;
	}
	
	/**
	 * Returns a list containing the packages that are not installed in the wanted environment
	 * from the list of dependencies provided
	 * 
	 * @param envName
	 * 	The name of the environment of interest. Should be one of the environments of the current Mamba instance.
	 * 	This parameter can also be the full path to an independent environment.
	 * @param dependencies
	 * 	The list of dependencies that should be installed in the environment.
	 * 	They can contain version requirements. The names should be the ones used to import the package inside python,
	 * 	"skimage", not "scikit-image" or "sklearn", not "scikit-learn"
	 * 	An example list: "numpy", "numba&gt;=0.43.1", "torch==1.6", "torch&gt;=1.6, &lt;2.0"
	 * @return true if the packages are installed or false otherwise
	 */
	public static List<String>  checkUninstalledDependenciesInEnv(String envName, List<String> dependencies) {
		File envFile = new File(this.envsdir, envName);
		File envFile2 = new File(envName);
		if (!envFile.isDirectory() && !envFile2.isDirectory())
			return dependencies;
		List<String> uninstalled = dependencies.stream().filter(dep -> {
			try {
				return !checkDependencyInEnv(envName, dep);
			} catch (Exception ex) {
				return true;
			}
		}).collect(Collectors.toList());
		return uninstalled;
	}
	
	/**
	 * Checks whether a package is installed in the wanted environment.
	 * TODO improve the logic for bigger or smaller versions
	 * 
	 * @param envName
	 * 	The name of the environment of interest. Should be one of the environments of the current Mamba instance.
	 * 	This parameter can also be the full path to an independent environment.
	 * @param dependency
	 * 	The name of the package that should be installed in the env
	 * 	They can contain version requirements. The names should be the ones used to import the package inside python,
	 * 	"skimage", not "scikit-image" or "sklearn", not "scikit-learn"
	 * 	An example list: "numpy", "numba&gt;=0.43.1", "torch==1.6", "torch&gt;=1.6, &lt;2.0"
	 * @return true if the package is installed or false otherwise
	 */
	public static boolean checkDependencyInEnv(String envName, String dependency) {
		if (dependency.contains("=<"))
			throw new IllegalArgumentException("=< is not valid, use <=");
		else if (dependency.contains("=>"))
			throw new IllegalArgumentException("=> is not valid, use >=");
		else if (dependency.contains(">") && dependency.contains("<") && !dependency.contains(","))
			throw new IllegalArgumentException("Invalid dependency format. To specify both a minimum and maximum version, "
					+ "separate the conditions with a comma. For example: 'torch>2.0.0, torch<2.5.0'.");
		
		if (dependency.contains("==")) {
			int ind = dependency.indexOf("==");
			return checkDependencyInEnv(envName, dependency.substring(0, ind).trim(), dependency.substring(ind + 2).trim());
		} else if (dependency.contains(">=") && dependency.contains("<=") && dependency.contains(",")) {
			int commaInd = dependency.indexOf(",");
			int highInd = dependency.indexOf(">=");
			int lowInd = dependency.indexOf("<=");
			int minInd = Math.min(Math.min(commaInd, lowInd), highInd);
			String packName = dependency.substring(0, minInd).trim();
			String maxV = dependency.substring(lowInd + 2, lowInd < highInd ? commaInd : dependency.length());
			String minV = dependency.substring(highInd + 2, lowInd < highInd ? dependency.length() : commaInd);
			if (maxV.equals("") || minV.equals(""))
				throw new IllegalArgumentException("Conditions must always begin with either '<' or '>' signs and then "
						+ "the version number. For example: 'torch>=2.0.0, torch<=2.5.0'.");
			return checkDependencyInEnv(envName, packName, minV, maxV, false);
		} else if (dependency.contains(">=") && dependency.contains("<") && dependency.contains(",")) {
			int commaInd = dependency.indexOf(",");
			int highInd = dependency.indexOf(">=");
			int lowInd = dependency.indexOf("<");
			int minInd = Math.min(Math.min(commaInd, lowInd), highInd);
			String packName = dependency.substring(0, minInd).trim();
			String maxV = dependency.substring(lowInd + 1, lowInd < highInd ? commaInd : dependency.length());
			String minV = dependency.substring(highInd + 2, lowInd < highInd ? dependency.length() : commaInd);
			if (maxV.equals("") || minV.equals(""))
				throw new IllegalArgumentException("Conditions must always begin with either '<' or '>' signs and then "
						+ "the version number. For example: 'torch>=2.0.0, torch<2.5.0'.");
			return checkDependencyInEnv(envName, packName, minV, null, false) && checkDependencyInEnv(envName, packName, null, maxV, true);
		} else if (dependency.contains(">") && dependency.contains("<=") && dependency.contains(",")) {
			int commaInd = dependency.indexOf(",");
			int highInd = dependency.indexOf(">");
			int lowInd = dependency.indexOf("<=");
			int minInd = Math.min(Math.min(commaInd, lowInd), highInd);
			String packName = dependency.substring(0, minInd).trim();
			String maxV = dependency.substring(lowInd + 2, lowInd < highInd ? commaInd : dependency.length());
			String minV = dependency.substring(highInd + 1, lowInd < highInd ? dependency.length() : commaInd);
			if (maxV.equals("") || minV.equals(""))
				throw new IllegalArgumentException("Conditions must always begin with either '<' or '>' signs and then "
						+ "the version number. For example: 'torch>2.0.0, torch<=2.5.0'.");
			return checkDependencyInEnv(envName, packName, minV, null, true) && checkDependencyInEnv(envName, packName, null, maxV, false);
		} else if (dependency.contains(">") && dependency.contains("<") && dependency.contains(",")) {
			int commaInd = dependency.indexOf(",");
			int highInd = dependency.indexOf(">");
			int lowInd = dependency.indexOf("<");
			int minInd = Math.min(Math.min(commaInd, lowInd), highInd);
			String packName = dependency.substring(0, minInd).trim();
			String maxV = dependency.substring(lowInd + 1, lowInd < highInd ? commaInd : dependency.length());
			String minV = dependency.substring(highInd + 1, lowInd < highInd ? dependency.length() : commaInd);
			if (maxV.equals("") || minV.equals(""))
				throw new IllegalArgumentException("Conditions must always begin with either '<' or '>' signs and then "
						+ "the version number. For example: 'torch>2.0.0, torch<2.5.0'.");
			return checkDependencyInEnv(envName, packName, minV, maxV, true);
		} else if (dependency.contains(">=")) {
			int ind = dependency.indexOf(">=");
			String maxV = dependency.substring(ind + 2).trim();
			if (maxV.equals(""))
				throw new IllegalArgumentException("Conditions must always begin with either '<' or '>' signs and then "
						+ "the version number. For example: 'torch>=2.0.0'.");
			return checkDependencyInEnv(envName, dependency.substring(0, ind).trim(), maxV, null, false);
		} else if (dependency.contains(">")) {
			int ind = dependency.indexOf(">");
			String maxV = dependency.substring(ind + 1).trim();
			if (maxV.equals(""))
				throw new IllegalArgumentException("Conditions must always begin with either '<' or '>' signs and then "
						+ "the version number. For example: 'torch>2.0.0'.");
			return checkDependencyInEnv(envName, dependency.substring(0, ind).trim(), maxV, null, true);
		} else if (dependency.contains("<=")) {
			int ind = dependency.indexOf("<=");
			String maxV = dependency.substring(ind + 2).trim();
			if (maxV.equals(""))
				throw new IllegalArgumentException("Conditions must always begin with either '<' or '>' signs and then "
						+ "the version number. For example: 'torch<=2.0.0'.");
			return checkDependencyInEnv(envName, dependency.substring(0, ind).trim(), null, maxV, false);
		} else if (dependency.contains("<")) {
			int ind = dependency.indexOf("<");
			String maxV = dependency.substring(ind + 1).trim();
			if (maxV.equals(""))
				throw new IllegalArgumentException("Conditions must always begin with either '<' or '>' signs and then "
						+ "the version number. For example: 'torch<2.0.0'.");
			return checkDependencyInEnv(envName, dependency.substring(0, ind).trim(), null, maxV, true);
		} else if (dependency.contains("=")) {
			int ind = dependency.indexOf("=");
			return checkDependencyInEnv(envName, dependency.substring(0, ind).trim(), dependency.substring(ind + 1).trim());
		}else {
			return checkDependencyInEnv(envName, dependency, null);
		}
	}
	
	/**
	 * Checks whether a package of a specific version is installed in the wanted environment.
	 * 
	 * @param envDir
	 * 	The directory of the environment of interest. Should be one of the environments of the current Mamba instance.
	 * 	This parameter can also be the full path to an independent environment.
	 * @param dependency
	 * 	The name of the package that should be installed in the env. The String should only contain the name, no version,
	 * 	and the name should be the one used to import the package inside python. For example, "skimage", not "scikit-image"
	 *  or "sklearn", not "scikit-learn".
	 * @param version
	 * 	the specific version of the package that needs to be installed. For example:, "0.43.1", "1.6", "2.0"
	 * @return true if the package is installed or false otherwise
	 */
	public static boolean checkDependencyInEnv(String envDir, String dependency, String version) throws MambaInstallException {
		return checkDependencyInEnv(envDir, dependency, version, version, true);
	}
	
	/**
	 * Checks whether a package with specific version constraints is installed in the wanted environment.
	 * In this method the minversion argument should be strictly smaller than the version of interest and
	 * the maxversion strictly bigger.
	 * This method checks that: dependency &gt;minversion, &lt;maxversion
	 * For smaller or equal or bigger or equal (dependency &gt;=minversion, &lt;=maxversion) look at the method
	 * {@link #checkDependencyInEnv(String, String, String, String, boolean)} with the lst parameter set to false.
	 * 
	 * @param envDir
	 * 	The directory of the environment of interest. Should be one of the environments of the current Mamba instance.
	 * 	This parameter can also be the full path to an independent environment.
	 * @param dependency
	 * 	The name of the package that should be installed in the env. The String should only contain the name, no version,
	 * 	and the name should be the one used to import the package inside python. For example, "skimage", not "scikit-image"
	 *  or "sklearn", not "scikit-learn".
	 * @param minversion
	 * 	the minimum required version of the package that needs to be installed. For example:, "0.43.1", "1.6", "2.0".
	 * 	This version should be strictly smaller than the one of interest, if for example "1.9" is given, it is assumed that
	 * 	package_version&gt;1.9.
	 * 	If there is no minimum version requirement for the package of interest, set this argument to null.
	 * @param maxversion
	 * 	the maximum required version of the package that needs to be installed. For example:, "0.43.1", "1.6", "2.0".
	 * 	This version should be strictly bigger than the one of interest, if for example "1.9" is given, it is assumed that
	 * 	package_version&lt;1.9.
	 * 	If there is no maximum version requirement for the package of interest, set this argument to null.
	 * @return true if the package is installed or false otherwise
	 */
	public static boolean checkDependencyInEnv(String envDir, String dependency, String minversion, String maxversion) {
		return checkDependencyInEnv(envDir, dependency, minversion, maxversion, true);
	}
	
	/**
	 * Checks whether a package with specific version constraints is installed in the wanted environment.
	 * Depending on the last argument ('strictlyBiggerOrSmaller') 'minversion' and 'maxversion'
	 * will be strictly bigger(&gt;=) or smaller(&lt;) or bigger or equal &gt;=) or smaller or equal&lt;=)
	 * In this method the minversion argument should be strictly smaller than the version of interest and
	 * the maxversion strictly bigger.
	 * 
	 * @param envDir
	 * 	The directory of the environment of interest. Should be one of the environments of the current Mamba instance.
	 * 	This parameter can also be the full path to an independent environment.
	 * @param dependency
	 * 	The name of the package that should be installed in the env. The String should only contain the name, no version,
	 * 	and the name should be the one used to import the package inside python. For example, "skimage", not "scikit-image"
	 *  or "sklearn", not "scikit-learn".
	 * @param minversion
	 * 	the minimum required version of the package that needs to be installed. For example:, "0.43.1", "1.6", "2.0".
	 * 	If there is no minimum version requirement for the package of interest, set this argument to null.
	 * @param maxversion
	 * 	the maximum required version of the package that needs to be installed. For example:, "0.43.1", "1.6", "2.0".
	 * 	If there is no maximum version requirement for the package of interest, set this argument to null.
	 * @param strictlyBiggerOrSmaller
	 * 	Whether the minversion and maxversion shuld be strictly smaller and bigger or not
	 * @return true if the package is installed or false otherwise
	 */
	public static boolean checkDependencyInEnv(String envDir, String dependency, String minversion, 
			String maxversion, boolean strictlyBiggerOrSmaller) {
		File envFile = new File(this.envsdir, envDir);
		File envFile2 = new File(envDir);
		if (!envFile.isDirectory() && !envFile2.isDirectory())
			return false;
		else if (!envFile.isDirectory())
			envFile = envFile2;
		if (dependency.trim().equals("python")) return checkPythonInstallation(envDir, minversion, maxversion, strictlyBiggerOrSmaller);
		String checkDepCode;
		if (minversion != null && maxversion != null && minversion.equals(maxversion)) {
			checkDepCode = "import importlib.util, sys; "
					+ "from importlib.metadata import version; "
					+ "from packaging import version as vv; "
					+ "pkg = '%s'; wanted_v = '%s'; "
					+ "spec = importlib.util.find_spec(pkg); "
					+ "vv_og = vv.parse(vv.parse(version('%s')).base_version); "
					+ "vv_nw = vv.parse(vv.parse(wanted_v).base_version); "
					+ "sys.exit(1) if spec is None else None; "
					+ "sys.exit(1) if vv_og != vv_nw else None; "
					+ "sys.exit(0);";
			checkDepCode = String.format(checkDepCode, resolveAliases(dependency), maxversion, dependency);
		} else if (minversion == null && maxversion == null) {
			checkDepCode = "import importlib.util, sys; sys.exit(0) if importlib.util.find_spec('%s') else sys.exit(1)";
			checkDepCode = String.format(checkDepCode, resolveAliases(dependency));
		} else if (maxversion == null) {
			checkDepCode = "import importlib.util, sys; "
					+ "from importlib.metadata import version; "
					+ "from packaging import version as vv; "
					+ "pkg = '%s'; desired_version = '%s'; "
					+ "spec = importlib.util.find_spec(pkg); "
					+ "curr_v = vv.parse(vv.parse(version('%s')).base_version); "
					+ "sys.exit(0) if spec and curr_v %s vv.parse(vv.parse(desired_version).base_version) else sys.exit(1)";
			checkDepCode = String.format(checkDepCode, resolveAliases(dependency), minversion, dependency, strictlyBiggerOrSmaller ? ">" : ">=");
		} else if (minversion == null) {
			checkDepCode = "import importlib.util, sys; "
					+ "from importlib.metadata import version; "
					+ "from packaging import version as vv; "
					+ "pkg = '%s'; desired_version = '%s'; "
					+ "spec = importlib.util.find_spec(pkg); "
					+ "curr_v = vv.parse(vv.parse(version('%s')).base_version); "
					+ "sys.exit(0) if spec and curr_v %s vv.parse(vv.parse(desired_version).base_version) else sys.exit(1)";
			checkDepCode = String.format(checkDepCode, resolveAliases(dependency), maxversion, dependency, strictlyBiggerOrSmaller ? "<" : "<=");
		} else {
			checkDepCode = "import importlib.util, sys; "
					+ "from importlib.metadata import version; "
					+ "from packaging import version as vv; "
					+ "pkg = '%s'; min_v = '%s'; max_v = '%s'; "
					+ "spec = importlib.util.find_spec(pkg); "
					+ "curr_v = vv.parse(vv.parse(version('%s')).base_version); "
					+ "sys.exit(0) if spec and curr_v %s vv.parse(vv.parse(min_v).base_version) and curr_v %s vv.parse(vv.parse(max_v).base_version) else sys.exit(1)";
			checkDepCode = String.format(checkDepCode, resolveAliases(dependency), minversion, maxversion, dependency,
					strictlyBiggerOrSmaller ? ">" : ">=", strictlyBiggerOrSmaller ? "<" : "<=");
		}
		try {
			runPythonIn(envFile, "-c", checkDepCode);
		} catch (RuntimeException | IOException | InterruptedException e) {
			return false;
		}
		return true;
	}
	
	private static String resolveAliases(String dep) {
		if (dep.equals("pytorch"))
			return "torch";
		else if (dep.equals("opencv-python"))
			return "cv2";
		else if (dep.equals("SAM-2"))
			return "sam2";
		else if (dep.equals("scikit-image"))
			return "skimage";
		else if (dep.equals("scikit-learn"))
			return "sklearn";
		return dep.replace("-", "_");
	}
	
	private static boolean checkPythonInstallation(String envDir, String minversion, String maxversion, boolean strictlyBiggerOrSmaller) throws MambaInstallException {
		File envFile = new File(this.envsdir, envDir);
		File envFile2 = new File(envDir);
		if (!envFile.isDirectory() && !envFile2.isDirectory())
			return false;
		else if (!envFile.isDirectory())
			envFile = envFile2;
		String checkDepCode;
		if (minversion != null && maxversion != null && minversion.equals(maxversion)) {
			checkDepCode = "import sys; import platform; from packaging import version as vv; desired_version = '%s'; "
					+ "sys.exit(0) if vv.parse(platform.python_version()).major == vv.parse(desired_version).major"
					+ " and vv.parse(platform.python_version()).minor == vv.parse(desired_version).minor else sys.exit(1)";
			checkDepCode = String.format(checkDepCode, maxversion);
		} else if (minversion == null && maxversion == null) {
			checkDepCode = "2 + 2";
		} else if (maxversion == null) {
			checkDepCode = "import sys; import platform; from packaging import version as vv; desired_version = '%s'; "
					+ "sys.exit(0) if vv.parse(platform.python_version()).major == vv.parse(desired_version).major "
					+ "and vv.parse(platform.python_version()).minor %s vv.parse(desired_version).minor else sys.exit(1)";
			checkDepCode = String.format(checkDepCode, minversion, strictlyBiggerOrSmaller ? ">" : ">=");
		} else if (minversion == null) {
			checkDepCode = "import sys; import platform; from packaging import version as vv; desired_version = '%s'; "
					+ "sys.exit(0) if vv.parse(platform.python_version()).major == vv.parse(desired_version).major "
					+ "and vv.parse(platform.python_version()).minor %s vv.parse(desired_version).minor else sys.exit(1)";
			checkDepCode = String.format(checkDepCode, maxversion, strictlyBiggerOrSmaller ? "<" : "<=");
		} else {
			checkDepCode = "import platform; "
					+ "from packaging import version as vv; min_v = '%s'; max_v = '%s'; "
					+ "sys.exit(0) if vv.parse(platform.python_version()).major == vv.parse(desired_version).major "
					+ "and vv.parse(platform.python_version()).minor %s vv.parse(min_v).minor "
					+ "and vv.parse(platform.python_version()).minor %s vv.parse(max_v).minor else sys.exit(1)";
			checkDepCode = String.format(checkDepCode, minversion, maxversion, strictlyBiggerOrSmaller ? ">" : ">=", strictlyBiggerOrSmaller ? "<" : ">=");
		}
		try {
			runPythonIn(envFile, "-c", checkDepCode);
		} catch (RuntimeException | IOException | InterruptedException e) {
			return false;
		}
		return true;
	}

}
