/*
 * Copyright (c) 2019 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at:
 *
 *     https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.jkube.kit.common.util;

import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;

/**
 * Utility class for analyzing Windows Portable Executable (PE) file headers.
 *
 * <p>This class provides methods to detect and parse PE headers from Windows executable files.
 * PE files start with a DOS header (MZ signature) followed by a PE header that contains
 * information about the executable's architecture, characteristics, and structure.</p>
 *
 * @author jkube
 */
public class PEHeaderUtil {

  private static final int DOS_HEADER_SIZE = 64;
  private static final int PE_SIGNATURE_OFFSET_POSITION = 0x3C;
  private static final int PE_SIGNATURE = 0x00004550; // "PE\0\0"
  private static final int MZ_SIGNATURE = 0x5A4D; // "MZ"
  private static final int COFF_HEADER_SIZE = 20;
  private static final int PE32_MAGIC = 0x10B;
  private static final int PE32_PLUS_MAGIC = 0x20B;

  private PEHeaderUtil() {
  }

  /**
   * Information extracted from a PE header.
   */
  @Getter
  public static class PEInfo {
    private final boolean valid;
    private final MachineType machineType;
    private final int numberOfSections;
    private final int characteristics;
    private final PEFormat format;
    private final Subsystem subsystem;
    private final long entryPoint;

    private PEInfo(boolean valid, MachineType machineType, int numberOfSections,
                   int characteristics, PEFormat format, Subsystem subsystem, long entryPoint) {
      this.valid = valid;
      this.machineType = machineType;
      this.numberOfSections = numberOfSections;
      this.characteristics = characteristics;
      this.format = format;
      this.subsystem = subsystem;
      this.entryPoint = entryPoint;
    }

    public boolean isExecutable() {
      return (characteristics & 0x0002) != 0;
    }

    public boolean isDll() {
      return (characteristics & 0x2000) != 0;
    }

    public boolean is32Bit() {
      return (characteristics & 0x0100) != 0;
    }

    public boolean isLargeAddressAware() {
      return (characteristics & 0x0020) != 0;
    }

    @Override
    public String toString() {
      return String.format("PEInfo{valid=%s, machineType=%s, numberOfSections=%d, " +
              "format=%s, subsystem=%s, isDll=%s, isExecutable=%s}",
          valid, machineType, numberOfSections, format, subsystem, isDll(), isExecutable());
    }
  }

  /**
   * PE file format types.
   */
  public enum PEFormat {
    PE32,
    PE32_PLUS,
    UNKNOWN
  }

  /**
   * Machine types as defined in the PE specification.
   */
  @Getter
  public enum MachineType {
    UNKNOWN(0x0, "Unknown"),
    I386(0x14c, "Intel 386"),
    ARM(0x1c0, "ARM little endian"),
    ARM64(0xaa64, "ARM64 little endian"),
    AMD64(0x8664, "x64"),
    IA64(0x200, "Intel Itanium");

    private final int code;
    private final String description;

    MachineType(int code, String description) {
      this.code = code;
      this.description = description;
    }

    public static MachineType fromCode(int code) {
      for (MachineType type : values()) {
        if (type.code == code) {
          return type;
        }
      }
      return UNKNOWN;
    }
  }

  /**
   * Subsystem types as defined in the PE specification.
   */
  @Getter
  public enum Subsystem {
    UNKNOWN(0, "Unknown"),
    NATIVE(1, "Native"),
    WINDOWS_GUI(2, "Windows GUI"),
    WINDOWS_CUI(3, "Windows Console"),
    OS2_CUI(5, "OS/2 Console"),
    POSIX_CUI(7, "POSIX Console"),
    WINDOWS_CE_GUI(9, "Windows CE"),
    EFI_APPLICATION(10, "EFI Application"),
    EFI_BOOT_SERVICE_DRIVER(11, "EFI Boot Service Driver"),
    EFI_RUNTIME_DRIVER(12, "EFI Runtime Driver"),
    EFI_ROM(13, "EFI ROM"),
    XBOX(14, "Xbox");

    private final int code;
    private final String description;

    Subsystem(int code, String description) {
      this.code = code;
      this.description = description;
    }

    public static Subsystem fromCode(int code) {
      for (Subsystem type : values()) {
        if (type.code == code) {
          return type;
        }
      }
      return UNKNOWN;
    }
  }

  /**
   * Checks if a file has the MZ signature (DOS header magic number).
   *
   * @param file the file to check
   * @return true if the file starts with MZ signature, false otherwise
   * @throws IOException if an I/O error occurs
   */
  public static boolean hasMZSignature(File file) throws IOException {
    try (InputStream is = Files.newInputStream(file.toPath())) {
      byte[] header = new byte[2];
      if (is.read(header) != 2) {
        return false;
      }
      return (header[0] == 'M' && header[1] == 'Z');
    }
  }

  /**
   * Checks if an input stream has the MZ signature (DOS header magic number).
   * Note: This method will read the first 2 bytes from the stream.
   *
   * @param is the input stream to check
   * @return true if the stream starts with MZ signature, false otherwise
   * @throws IOException if an I/O error occurs
   */
  public static boolean hasMZSignature(InputStream is) throws IOException {
    byte[] header = new byte[2];
    if (is.read(header) != 2) {
      return false;
    }
    return (header[0] == 'M' && header[1] == 'Z');
  }

  /**
   * Analyzes a PE file and extracts header information.
   *
   * @param file the file to analyze
   * @return PEInfo object containing the parsed header information
   * @throws IOException if an I/O error occurs
   */
  public static PEInfo analyzePEHeader(File file) throws IOException {
    try (InputStream is = Files.newInputStream(file.toPath())) {
      return analyzePEHeader(is);
    }
  }

  /**
   * Analyzes a PE input stream and extracts header information.
   *
   * @param is the input stream to analyze
   * @return PEInfo object containing the parsed header information
   * @throws IOException if an I/O error occurs
   */
  public static PEInfo analyzePEHeader(InputStream is) throws IOException {
    Integer peHeaderOffset = validateDOSHeader(is);
    if (peHeaderOffset == null) {
      return createInvalidPEInfo();
    }

    if (!skipToPEHeader(is, peHeaderOffset)) {
      return createInvalidPEInfo();
    }

    ByteBuffer peBuffer = readAndValidatePESignature(is);
    if (peBuffer == null) {
      return createInvalidPEInfo();
    }

    COFFHeaderData coffData = parseCOFFHeader(peBuffer);
    OptionalHeaderData optData = parseOptionalHeader(is, coffData.sizeOfOptionalHeader);

    return new PEInfo(true, coffData.machineType, coffData.numberOfSections,
        coffData.characteristics, optData.format, optData.subsystem, optData.entryPoint);
  }

  private static PEInfo createInvalidPEInfo() {
    return new PEInfo(false, MachineType.UNKNOWN, 0, 0, PEFormat.UNKNOWN, Subsystem.UNKNOWN, 0);
  }

  private static Integer validateDOSHeader(InputStream is) throws IOException {
    byte[] dosHeader = new byte[DOS_HEADER_SIZE];
    if (is.read(dosHeader) != DOS_HEADER_SIZE) {
      return null;
    }

    ByteBuffer dosBuffer = ByteBuffer.wrap(dosHeader).order(ByteOrder.LITTLE_ENDIAN);
    int mzSignature = dosBuffer.getShort(0) & 0xFFFF;
    if (mzSignature != MZ_SIGNATURE) {
      return null;
    }

    return dosBuffer.getInt(PE_SIGNATURE_OFFSET_POSITION);
  }

  private static boolean skipToPEHeader(InputStream is, int peHeaderOffset) throws IOException {
    long bytesToSkip = peHeaderOffset - DOS_HEADER_SIZE;
    if (bytesToSkip > 0) {
      long skipped = is.skip(bytesToSkip);
      return skipped == bytesToSkip;
    }
    return true;
  }

  private static ByteBuffer readAndValidatePESignature(InputStream is) throws IOException {
    byte[] peSignatureAndCoff = new byte[4 + COFF_HEADER_SIZE];
    if (is.read(peSignatureAndCoff) != peSignatureAndCoff.length) {
      return null;
    }

    ByteBuffer peBuffer = ByteBuffer.wrap(peSignatureAndCoff).order(ByteOrder.LITTLE_ENDIAN);
    int peSignature = peBuffer.getInt(0);
    if (peSignature != PE_SIGNATURE) {
      return null;
    }

    return peBuffer;
  }

  private static COFFHeaderData parseCOFFHeader(ByteBuffer peBuffer) {
    int machine = peBuffer.getShort(4) & 0xFFFF;
    int numberOfSections = peBuffer.getShort(6) & 0xFFFF;
    int sizeOfOptionalHeader = peBuffer.getShort(20) & 0xFFFF;
    int characteristics = peBuffer.getShort(22) & 0xFFFF;
    MachineType machineType = MachineType.fromCode(machine);

    return new COFFHeaderData(machineType, numberOfSections, characteristics, sizeOfOptionalHeader);
  }

  private static OptionalHeaderData parseOptionalHeader(InputStream is, int sizeOfOptionalHeader)
      throws IOException {
    if (sizeOfOptionalHeader == 0) {
      return new OptionalHeaderData(PEFormat.UNKNOWN, Subsystem.UNKNOWN, 0);
    }

    byte[] optionalHeader = new byte[Math.min(sizeOfOptionalHeader, 96)];
    if (is.read(optionalHeader) <= 0) {
      return new OptionalHeaderData(PEFormat.UNKNOWN, Subsystem.UNKNOWN, 0);
    }

    ByteBuffer optBuffer = ByteBuffer.wrap(optionalHeader).order(ByteOrder.LITTLE_ENDIAN);
    int magic = optBuffer.getShort(0) & 0xFFFF;

    PEFormat format = determinePEFormat(magic);
    Subsystem subsystem = extractSubsystem(optBuffer, optionalHeader.length);
    long entryPoint = extractEntryPoint(optBuffer, optionalHeader.length);

    return new OptionalHeaderData(format, subsystem, entryPoint);
  }

  private static PEFormat determinePEFormat(int magic) {
    if (magic == PE32_MAGIC) {
      return PEFormat.PE32;
    } else if (magic == PE32_PLUS_MAGIC) {
      return PEFormat.PE32_PLUS;
    }
    return PEFormat.UNKNOWN;
  }

  private static Subsystem extractSubsystem(ByteBuffer buffer, int length) {
    if (length >= 68) {
      return Subsystem.fromCode(buffer.getShort(68) & 0xFFFF);
    }
    return Subsystem.UNKNOWN;
  }

  private static long extractEntryPoint(ByteBuffer buffer, int length) {
    if (length >= 16) {
      return buffer.getInt(16) & 0xFFFFFFFFL;
    }
    return 0;
  }

  private static class COFFHeaderData {
    final MachineType machineType;
    final int numberOfSections;
    final int characteristics;
    final int sizeOfOptionalHeader;

    COFFHeaderData(MachineType machineType, int numberOfSections,
                   int characteristics, int sizeOfOptionalHeader) {
      this.machineType = machineType;
      this.numberOfSections = numberOfSections;
      this.characteristics = characteristics;
      this.sizeOfOptionalHeader = sizeOfOptionalHeader;
    }
  }

  private static class OptionalHeaderData {
    final PEFormat format;
    final Subsystem subsystem;
    final long entryPoint;

    OptionalHeaderData(PEFormat format, Subsystem subsystem, long entryPoint) {
      this.format = format;
      this.subsystem = subsystem;
      this.entryPoint = entryPoint;
    }
  }

  /**
   * Checks if a file is a valid Windows PE executable.
   *
   * @param file the file to check
   * @return true if the file is a valid PE executable, false otherwise
   */
  public static boolean isPEFile(File file) {
    try {
      PEInfo info = analyzePEHeader(file);
      return info.isValid();
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Checks if an input stream contains a valid Windows PE executable.
   *
   * @param is the input stream to check
   * @return true if the stream contains a valid PE executable, false otherwise
   */
  public static boolean isPEFile(InputStream is) {
    try {
      PEInfo info = analyzePEHeader(is);
      return info.isValid();
    } catch (IOException e) {
      return false;
    }
  }
}