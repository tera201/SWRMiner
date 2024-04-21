package org.repodriller.scm.entities;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class BlameManager {
    Map<String, BlameFileInfo> fileMap;
    Map<String, BlamePackageInfo> packageMap;
    BlamePackageInfo rootPackageInfo;
    String projectName;

    public BlameManager(Map<String, BlameFileInfo> fileMap, String projectName) {
        this.fileMap = fileMap;
        this.projectName = projectName;
        this.packageMap = aggregateByPackage();
    }

    private Map<String, BlamePackageInfo> aggregateByPackage() {
        Map<String, BlamePackageInfo> packageData = new HashMap<>();

        for (Map.Entry<String, BlameFileInfo> entry : fileMap.entrySet()) {
            String filePath = "/" + projectName + "/" + entry.getKey();
            BlameFileInfo fileInfo = entry.getValue();
            String packagePath = (filePath.lastIndexOf('/') > 0 ) ? filePath.substring(0, filePath.lastIndexOf('/')) : "";

            while (packagePath.lastIndexOf('/') != -1) {
                String packageName = (packagePath.lastIndexOf('/') != -1 ) ? packagePath.substring(packagePath.lastIndexOf('/') + 1) : packagePath;
                packageData.computeIfAbsent(packagePath, k -> new BlamePackageInfo(packageName)).add(fileInfo, filePath);
                packagePath = switch (packagePath.lastIndexOf('/')) {
                    case -1, 0 -> "";
                    default -> packagePath.substring(0, packagePath.lastIndexOf('/'));
                };
            }
        }
        rootPackageInfo = packageData.get("/" + projectName);
        return packageData;
    }
}
