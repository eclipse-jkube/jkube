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

import org.eclipse.jkube.kit.common.util.PEHeaderUtil.MachineType;
import org.eclipse.jkube.kit.common.util.PEHeaderUtil.PEFormat;
import org.eclipse.jkube.kit.common.util.PEHeaderUtil.PEInfo;
import org.eclipse.jkube.kit.common.util.PEHeaderUtil.Subsystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

class PEHeaderUtilTest {

  @TempDir
  File tempDir;

  @Test
  void hasMZSignature_withValidMZFile_shouldReturnTrue() throws IOException {
    // Given
    File peFile = createMinimalPEFile(MachineType.AMD64, false);

    // When
    boolean result = PEHeaderUtil.hasMZSignature(peFile);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void hasMZSignature_withInvalidFile_shouldReturnFalse() throws IOException {
    // Given
    File nonPeFile = new File(tempDir, "nonpe.bin");
    Files.write(nonPeFile.toPath(), new byte[]{0x00, 0x00, 0x01, 0x02});

    // When
    boolean result = PEHeaderUtil.hasMZSignature(nonPeFile);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void hasMZSignature_withEmptyFile_shouldReturnFalse() throws IOException {
    // Given
    File emptyFile = new File(tempDir, "empty.bin");
    Files.write(emptyFile.toPath(), new byte[]{});

    // When
    boolean result = PEHeaderUtil.hasMZSignature(emptyFile);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void hasMZSignature_withInputStream_shouldReturnTrue() throws IOException {
    // Given
    byte[] header = new byte[]{'M', 'Z'};
    InputStream is = new ByteArrayInputStream(header);

    // When
    boolean result = PEHeaderUtil.hasMZSignature(is);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void analyzePEHeader_withValidPE32File_shouldReturnValidInfo() throws IOException {
    // Given
    File peFile = createMinimalPEFile(MachineType.I386, false);

    // When
    PEInfo info = PEHeaderUtil.analyzePEHeader(peFile);

    // Then
    assertThat(info.isValid()).isTrue();
    assertThat(info.getMachineType()).isEqualTo(MachineType.I386);
    assertThat(info.getFormat()).isEqualTo(PEFormat.PE32);
    assertThat(info.isExecutable()).isTrue();
    assertThat(info.isDll()).isFalse();
  }

  @Test
  void analyzePEHeader_withValidPE32PlusFile_shouldReturnValidInfo() throws IOException {
    // Given
    File peFile = createMinimalPEFile(MachineType.AMD64, false);

    // When
    PEInfo info = PEHeaderUtil.analyzePEHeader(peFile);

    // Then
    assertThat(info.isValid()).isTrue();
    assertThat(info.getMachineType()).isEqualTo(MachineType.AMD64);
    assertThat(info.getFormat()).isEqualTo(PEFormat.PE32_PLUS);
    assertThat(info.isExecutable()).isTrue();
  }

  @Test
  void analyzePEHeader_withDLL_shouldReturnDllInfo() throws IOException {
    // Given
    File dllFile = createMinimalPEFile(MachineType.AMD64, true);

    // When
    PEInfo info = PEHeaderUtil.analyzePEHeader(dllFile);

    // Then
    assertThat(info.isValid()).isTrue();
    assertThat(info.isDll()).isTrue();
    assertThat(info.isExecutable()).isTrue();
  }

  @Test
  void analyzePEHeader_withARM64File_shouldReturnARM64Info() throws IOException {
    // Given
    File peFile = createMinimalPEFile(MachineType.ARM64, false);

    // When
    PEInfo info = PEHeaderUtil.analyzePEHeader(peFile);

    // Then
    assertThat(info.isValid()).isTrue();
    assertThat(info.getMachineType()).isEqualTo(MachineType.ARM64);
  }

  @Test
  void analyzePEHeader_withInvalidFile_shouldReturnInvalidInfo() throws IOException {
    // Given
    File invalidFile = new File(tempDir, "invalid.bin");
    Files.write(invalidFile.toPath(), new byte[]{0x00, 0x01, 0x02, 0x03});

    // When
    PEInfo info = PEHeaderUtil.analyzePEHeader(invalidFile);

    // Then
    assertThat(info.isValid()).isFalse();
    assertThat(info.getMachineType()).isEqualTo(MachineType.UNKNOWN);
  }

  @Test
  void analyzePEHeader_withInputStream_shouldReturnValidInfo() throws IOException {
    // Given
    byte[] peData = createMinimalPEBytes(MachineType.AMD64, false);
    InputStream is = new ByteArrayInputStream(peData);

    // When
    PEInfo info = PEHeaderUtil.analyzePEHeader(is);

    // Then
    assertThat(info.isValid()).isTrue();
    assertThat(info.getMachineType()).isEqualTo(MachineType.AMD64);
  }

  @Test
  void isPEFile_withValidPEFile_shouldReturnTrue() throws IOException {
    // Given
    File peFile = createMinimalPEFile(MachineType.AMD64, false);

    // When
    boolean result = PEHeaderUtil.isPEFile(peFile);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void isPEFile_withInvalidFile_shouldReturnFalse() throws IOException {
    // Given
    File invalidFile = new File(tempDir, "invalid.bin");
    Files.write(invalidFile.toPath(), new byte[]{0x00, 0x01, 0x02, 0x03});

    // When
    boolean result = PEHeaderUtil.isPEFile(invalidFile);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void isPEFile_withInputStream_shouldReturnTrue() {
    // Given
    byte[] peData = createMinimalPEBytes(MachineType.AMD64, false);
    InputStream is = new ByteArrayInputStream(peData);

    // When
    boolean result = PEHeaderUtil.isPEFile(is);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void machineType_fromCode_shouldReturnCorrectType() {
    assertThat(MachineType.fromCode(0x14c)).isEqualTo(MachineType.I386);
    assertThat(MachineType.fromCode(0x8664)).isEqualTo(MachineType.AMD64);
    assertThat(MachineType.fromCode(0xaa64)).isEqualTo(MachineType.ARM64);
    assertThat(MachineType.fromCode(0x1c0)).isEqualTo(MachineType.ARM);
    assertThat(MachineType.fromCode(0x200)).isEqualTo(MachineType.IA64);
    assertThat(MachineType.fromCode(0x9999)).isEqualTo(MachineType.UNKNOWN);
  }

  @Test
  void machineType_getDescription_shouldReturnCorrectDescription() {
    assertThat(MachineType.I386.getDescription()).isEqualTo("Intel 386");
    assertThat(MachineType.AMD64.getDescription()).isEqualTo("x64");
    assertThat(MachineType.ARM64.getDescription()).isEqualTo("ARM64 little endian");
  }

  @Test
  void subsystem_fromCode_shouldReturnCorrectType() {
    assertThat(Subsystem.fromCode(1)).isEqualTo(Subsystem.NATIVE);
    assertThat(Subsystem.fromCode(2)).isEqualTo(Subsystem.WINDOWS_GUI);
    assertThat(Subsystem.fromCode(3)).isEqualTo(Subsystem.WINDOWS_CUI);
    assertThat(Subsystem.fromCode(10)).isEqualTo(Subsystem.EFI_APPLICATION);
    assertThat(Subsystem.fromCode(9999)).isEqualTo(Subsystem.UNKNOWN);
  }

  @Test
  void peInfo_toString_shouldReturnFormattedString() throws IOException {
    // Given
    File peFile = createMinimalPEFile(MachineType.AMD64, false);

    // When
    PEInfo info = PEHeaderUtil.analyzePEHeader(peFile);
    String result = info.toString();

    // Then
    assertThat(result)
        .contains("valid=true")
        .contains("machineType=AMD64")
        .contains("format=PE32_PLUS")
        .contains("isExecutable=true");
  }

  @Test
  void peInfo_characteristics_shouldReflectFileProperties() throws IOException {
    // Given
    File peFile = createMinimalPEFile(MachineType.I386, false);

    // When
    PEInfo info = PEHeaderUtil.analyzePEHeader(peFile);

    // Then
    assertThat(info.is32Bit()).isTrue();
    assertThat(info.isLargeAddressAware()).isFalse();
  }

  @Test
  void analyzePEHeader_withShortFile_shouldReturnInvalidInfo() throws IOException {
    // Given
    File shortFile = new File(tempDir, "short.bin");
    Files.write(shortFile.toPath(), new byte[]{'M', 'Z', 0x00});

    // When
    PEInfo info = PEHeaderUtil.analyzePEHeader(shortFile);

    // Then
    assertThat(info.isValid()).isFalse();
  }

  @Test
  void analyzePEHeader_withWrongPESignature_shouldReturnInvalidInfo() throws IOException {
    // Given
    File wrongPEFile = createFileWithWrongPESignature();

    // When
    PEInfo info = PEHeaderUtil.analyzePEHeader(wrongPEFile);

    // Then
    assertThat(info.isValid()).isFalse();
  }

  /**
   * Creates a minimal valid PE file for testing purposes.
   *
   * @param machineType the machine type for the PE file
   * @param isDll       whether the file should be marked as a DLL
   * @return the created PE file
   * @throws IOException if file creation fails
   */
  private File createMinimalPEFile(MachineType machineType, boolean isDll) throws IOException {
    byte[] peData = createMinimalPEBytes(machineType, isDll);
    File peFile = new File(tempDir, "test.exe");
    Files.write(peFile.toPath(), peData);
    return peFile;
  }

  /**
   * Creates minimal valid PE file bytes for testing.
   *
   * @param machineType the machine type for the PE file
   * @param isDll       whether the file should be marked as a DLL
   * @return byte array representing a minimal PE file
   */
  private byte[] createMinimalPEBytes(MachineType machineType, boolean isDll) {
    ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);

    // DOS Header
    buffer.putShort((short) 0x5A4D); // MZ signature
    buffer.position(0x3C);
    buffer.putInt(0x80); // PE header offset at position 128

    // Skip to PE header position (128)
    buffer.position(0x80);

    // PE Signature
    buffer.putInt(0x00004550); // "PE\0\0"

    // COFF Header
    buffer.putShort((short) machineType.getCode()); // Machine type
    buffer.putShort((short) 3); // Number of sections
    buffer.putInt(0); // TimeDateStamp
    buffer.putInt(0); // PointerToSymbolTable
    buffer.putInt(0); // NumberOfSymbols
    buffer.putShort((short) 96); // SizeOfOptionalHeader

    // Characteristics
    int characteristics = 0x0002; // Executable
    if (isDll) {
      characteristics |= 0x2000; // DLL
    }
    if (machineType == MachineType.I386) {
      characteristics |= 0x0100; // 32-bit
    }
    buffer.putShort((short) characteristics);

    // Optional Header
    if (machineType == MachineType.AMD64 || machineType == MachineType.ARM64 || machineType == MachineType.IA64) {
      buffer.putShort((short) 0x20B); // PE32+ magic
    } else {
      buffer.putShort((short) 0x10B); // PE32 magic
    }
    buffer.put((byte) 14); // MajorLinkerVersion
    buffer.put((byte) 0);  // MinorLinkerVersion
    buffer.putInt(0x1000); // SizeOfCode
    buffer.putInt(0x1000); // SizeOfInitializedData
    buffer.putInt(0); // SizeOfUninitializedData
    buffer.putInt(0x1000); // AddressOfEntryPoint
    buffer.putInt(0x1000); // BaseOfCode

    // Fill rest of optional header
    buffer.position(buffer.position() + 40);
    buffer.putShort((short) 3); // Subsystem: Windows Console

    return buffer.array();
  }

  /**
   * Creates a file with MZ signature but wrong PE signature for testing.
   */
  private File createFileWithWrongPESignature() throws IOException {
    ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);

    // DOS Header with MZ signature
    buffer.putShort((short) 0x5A4D); // MZ signature
    buffer.position(0x3C);
    buffer.putInt(0x80); // PE header offset

    // Skip to PE header position
    buffer.position(0x80);

    // Wrong PE Signature
    buffer.putInt(0x12345678); // Not "PE\0\0"

    File file = new File(tempDir, "wrong_pe.exe");
    Files.write(file.toPath(), buffer.array());
    return file;
  }
}