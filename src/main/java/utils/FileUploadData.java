package utils;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import objects.utils.ImportHistoryUnit;

import java.io.InputStream;

@Entity
@Table(name = "import_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column
    private String fileName;
    @Column
    private String originalFileName;
    @Column
    private long fileSize;
    @Column
    private String contentType;
    @ManyToOne
    @JoinColumn(name = "import_history_unit_id")
    private ImportHistoryUnit importHistoryUnit;

    @Transient
    private InputStream inputStream;

    public FileUploadData(String originalFileName, long fileSize, String contentType, InputStream inputStream) {
        this.originalFileName = originalFileName;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.inputStream = inputStream;
    }
}
