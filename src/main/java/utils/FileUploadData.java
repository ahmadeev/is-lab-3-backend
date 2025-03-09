package utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.InputStream;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class FileUploadData {
    private InputStream inputStream;
    private String fileName;
    private long fileSize;
    private String contentType;
}
