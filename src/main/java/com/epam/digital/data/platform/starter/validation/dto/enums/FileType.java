package com.epam.digital.data.platform.starter.validation.dto.enums;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enumeration of available file types on the user form
 */
@AllArgsConstructor
@Getter
public enum FileType {

  FILE_TYPE("file"),
  LATEST_FILE_TYPE("fileLatest"),
  LEGACY_FILE_TYPE("fileLegacy");

  private final String name;

  public static boolean isValidFileType(String fileType) {
    return Arrays.stream(values()).anyMatch(validType -> validType.getName().equals(fileType));
  }
}
