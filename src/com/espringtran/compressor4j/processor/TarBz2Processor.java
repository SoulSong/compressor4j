/*
 * Copyright (C) 2013-2015 E-Spring Tran
 * 
 *             https://espringtran.com
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.espringtran.compressor4j.processor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

import com.espringtran.compressor4j.bean.BinaryFile;
import com.espringtran.compressor4j.compressor.FileCompressor;
import com.espringtran.compressor4j.util.FileUtil;
import com.espringtran.compressor4j.util.LogUtil;

/**
 * 
 * @author E-Spring Tran
 * 
 */
public class TarBz2Processor implements CompressProcessor {

    /**
     * Compress data
     * 
     * @param fileCompressor
     *            FileCompressor object
     * @return
     * @throws Exception
     */
    @Override
    public byte[] compressData(FileCompressor fileCompressor) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BZip2CompressorOutputStream cos = new BZip2CompressorOutputStream(baos);
        TarArchiveOutputStream aos = new TarArchiveOutputStream(cos);
        try {
            for (BinaryFile binaryFile : fileCompressor.getMapBinaryFile()
                    .values()) {
                TarArchiveEntry entry = new TarArchiveEntry(
                        binaryFile.getDesPath());
                entry.setSize(binaryFile.getActualSize());
                aos.putArchiveEntry(entry);
                aos.write(binaryFile.getData());
                aos.closeArchiveEntry();
            }
            aos.flush();
            aos.finish();
        } catch (Exception e) {
            FileCompressor.LOGGER.error("Error on compress data", e);
        } finally {
            aos.close();
            cos.close();
            baos.close();
        }
        return baos.toByteArray();
    }

    /**
     * Read from compressed file
     * 
     * @param srcPath
     *            path of compressed file
     * @param fileCompressor
     *            FileCompressor object
     * @throws Exception
     */
    @Override
    public void read(String srcPath, FileCompressor fileCompressor)
            throws Exception {
        long t1 = System.currentTimeMillis();
        byte[] data = FileUtil.convertFileToByte(srcPath);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        BZip2CompressorInputStream cis = new BZip2CompressorInputStream(bais);
        TarArchiveInputStream ais = new TarArchiveInputStream(cis);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[1024];
            int readByte;
            TarArchiveEntry entry = ais.getNextTarEntry();
            while (entry != null && entry.getSize() > 0) {
                long t2 = System.currentTimeMillis();
                baos = new ByteArrayOutputStream();
                readByte = ais.read(buffer);
                while (readByte != -1) {
                    baos.write(buffer, 0, readByte);
                    readByte = ais.read(buffer);
                }
                BinaryFile binaryFile = new BinaryFile(entry.getName(),
                        baos.toByteArray());
                fileCompressor.addBinaryFile(binaryFile);
                LogUtil.createAddFileLog(fileCompressor, binaryFile, t2,
                        System.currentTimeMillis());
                entry = ais.getNextTarEntry();
            }
        } catch (Exception e) {
            FileCompressor.LOGGER.error("Error on get compressor file", e);
        } finally {
            baos.close();
            ais.close();
            cis.close();
            bais.close();
        }
        LogUtil.createReadLog(fileCompressor, srcPath, data.length, t1,
                System.currentTimeMillis());
    }

}
