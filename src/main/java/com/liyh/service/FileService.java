package com.liyh.service;

import com.liyh.entity.DownloadFileInfo;
import com.liyh.entity.UploadFileInfo;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FileService {

    private static final String UTF_8 = "UTF-8";
    private String uploadPath = System.getProperty("user.dir") + "/springboot-file/upload/";

    private ConcurrentHashMap<String, Integer> fileUploadProgressMap = new ConcurrentHashMap<>();

    public void upload(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ServletFileUpload servletFileUpload = getServletFileUpload();
        List<FileItem> items = servletFileUpload.parseRequest(request);
        UploadFileInfo uploadFileInfo = getFileInfo(items);
        writeTempFile(items, uploadFileInfo);
        mergeFile(uploadFileInfo);
        response.setCharacterEncoding(UTF_8);
        response.getWriter().write("上传成功");
    }

    private ServletFileUpload getServletFileUpload() {
        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold(1024);
        File file = new File(uploadPath);
        if (!file.exists() && !file.isDirectory()) {
            file.mkdirs();
        }
        factory.setRepository(file);
        ServletFileUpload upload = new ServletFileUpload(factory);
        upload.setFileSizeMax(1 * 1024 * 1024 * 1024L);
        upload.setSizeMax(10 * 1024 * 1024 * 1024L);
        return upload;
    }

    private UploadFileInfo getFileInfo(List<FileItem> items) throws UnsupportedEncodingException {
        UploadFileInfo uploadFileInfo = new UploadFileInfo();
        for (FileItem item : items) {
            if (item.isFormField()) {
                if ("chunk".equals(item.getFieldName())) {
                    uploadFileInfo.setCurrentChunk(Integer.parseInt(item.getString(UTF_8)));
                }
                if ("chunks".equals(item.getFieldName())) {
                    uploadFileInfo.setChunks(Integer.parseInt(item.getString(UTF_8)));
                }
                if ("name".equals(item.getFieldName())) {
                    uploadFileInfo.setFileName(item.getString(UTF_8));
                }
            }
        }
        return uploadFileInfo;
    }

    private void writeTempFile(List<FileItem> items, UploadFileInfo uploadFileInfo) throws Exception {
        for (FileItem item : items) {
            if (!item.isFormField()) {
                String tempFileName = uploadFileInfo.getFileName();
                if (StringUtils.isNotBlank(tempFileName)) {
                    if (uploadFileInfo.getCurrentChunk() != null) {
                        tempFileName = uploadFileInfo.getCurrentChunk() + "_" + uploadFileInfo.getFileName();
                    }
                    File tempFile = new File(uploadPath, tempFileName);
                    if (!tempFile.exists()) {
                        item.write(tempFile);
                        fileUploadProgressMap.merge(uploadFileInfo.getFileName(), 1, Integer::sum);
                        int uploadedChunks = fileUploadProgressMap.get(uploadFileInfo.getFileName());
                        System.out.println("已上传 " + uploadedChunks + " 个分片。");
                    }
                }
            }
        }
    }

    private void mergeFile(UploadFileInfo uploadFileInfo) throws IOException, InterruptedException {
        Integer currentChunk = uploadFileInfo.getCurrentChunk();
        Integer chunks = uploadFileInfo.getChunks();
        String fileName = uploadFileInfo.getFileName();
        if (currentChunk != null && chunks != null && currentChunk.equals(chunks - 1)) {
            File tempFile = new File(uploadPath, fileName);
            try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(tempFile))) {
                for (int i = 0; i < chunks; i++) {
                    File file = new File(uploadPath, i + "_" + fileName);
                    while (!file.exists()) {
                        Thread.sleep(100);
                    }
                    byte[] bytes = FileUtils.readFileToByteArray(file);
                    os.write(bytes);
                    os.flush();
                    file.delete();
                }
                os.flush();
            }
            fileUploadProgressMap.remove(fileName);
            System.out.println("文件上传完成。");
        }
    }

    public void download(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String downloadFile = "";  // 请设置 downloadFile 的路径
        File file = new File(downloadFile);
        DownloadFileInfo downloadFileInfo = getDownloadFileInfo(file.length(), request, response);
        setResponse(response, file.getName(), downloadFileInfo);
        try (InputStream is = new BufferedInputStream(new FileInputStream(file));
             OutputStream os = new BufferedOutputStream(response.getOutputStream())) {
            is.skip(downloadFileInfo.getPos());
            byte[] buffer = new byte[1024];
            long sum = 0;
            while (sum < downloadFileInfo.getRangeLength()) {
                int length = is.read(buffer, 0, (downloadFileInfo.getRangeLength() - sum) <= buffer.length ? (int) (downloadFileInfo.getRangeLength() - sum) : buffer.length);
                sum += length;
                os.write(buffer, 0, length);
            }
        }
    }

    public void downloads() {
        // Implement the logic for segmented file download
    }

    private DownloadFileInfo getDownloadFileInfo(long fileLength, HttpServletRequest request, HttpServletResponse response) {
        // Implement the method to get download file info
        return new DownloadFileInfo();
    }

    private void setResponse(HttpServletResponse response, String fileName, DownloadFileInfo downloadFileInfo) {
        // Implement the method to set response headers
    }
}
