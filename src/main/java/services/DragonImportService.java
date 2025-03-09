package services;

import auth.Roles;
import auth.User;
import dto.utils.ImportHistoryUnitDTO;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.transaction.Transactional;
import jakarta.transaction.UserTransaction;
import objects.*;
import objects.utils.ImportHistoryUnit;
import objects.utils.ImportStatus;
import org.apache.commons.compress.utils.IOUtils;
import repositories.ImportHistoryRepository;
import utils.FileUploadData;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;


@Named(value = "dragonImportService")
@ApplicationScoped
public class DragonImportService {
    @Inject
    private DragonService dragonService;

    @Inject
    private ImportHistoryRepository importHistoryRepository;

    @Inject
    private MinioService minioService;

    @Resource
    private UserTransaction userTransaction; // Программное управление транзакциями

    // двухфазный коммит (старый метод на месте, но не используется)
    public void importDragonsFromCsvWith2PC(List<FileUploadData> fileUploads, User user) throws Exception {
        Map<String, String> tempToPerm = new HashMap<>();
        int dragonsCount = 0;
        List<FileUploadData> filesToUpload = new ArrayList<>();

        // запись в журнале
        ImportHistoryUnit importHistoryUnit = new ImportHistoryUnit();
        importHistoryUnit.setUser(user);
        importHistoryUnit.setStatus(ImportStatus.PENDING);
        importHistoryUnit.setRowsAdded(dragonsCount);
        importHistoryUnit.setFiles(filesToUpload);

        // TODO: вне транзакции эээээ // propaganation из ejb ???
        importHistoryRepository.save(importHistoryUnit);

        try {
            userTransaction.begin();

            // обходим список метаданных файлов (кастомное)
            for (FileUploadData fileData : fileUploads) {
                String originalFileName = fileData.getOriginalFileName();
                String contentType = fileData.getContentType();
                // читаем содержимое файла в массив байтов для повторного использования потока
                byte[] fileBytes = IOUtils.toByteArray(fileData.getInputStream());
                // fix: размер считаем сами
                long fileSize = Math.max(fileData.getFileSize(), fileBytes.length);

                // todo: костылёк в целом
                fileData.setFileSize(fileSize);

                // генерируем временное и постоянное имена
                String tempFileName = "temp_" + UUID.randomUUID() + "_" + originalFileName;
                String permanentFileName = "import_" + UUID.randomUUID() + "_" + originalFileName;
                // fix
                tempToPerm.put(tempFileName, permanentFileName);

                // todo: костылёк в целом
                fileData.setFileName(permanentFileName);
                // System.out.println(fileData.toString());
                filesToUpload.add(fileData);

                List<Dragon> allDragons = new ArrayList<>();
                try {
                    System.out.println("parsing file: " + originalFileName);
                    // парсинг CSV
                    ByteArrayInputStream parseStream = new ByteArrayInputStream(fileBytes);
                    List<Dragon> dragons = parseDragonsFromCsv(parseStream, user);
                    allDragons.addAll(dragons);

                    System.out.println("validating file: " + originalFileName);
                    // валидация драконов. не будет даже попытки добавить в БД до прохождения валидации каждым драконом
                    for (Dragon dragon : allDragons) {
                        if (!dragon.isValid()) {
                            throw new Exception("Invalid dragon found in file " + originalFileName);
                        }
                    }
                    dragonsCount += allDragons.size();

                    System.out.println("saving file in DB: " + originalFileName);
                    // сохранение драконов в БД. типа препэйр
                    dragonService.createAll(allDragons);

                    System.out.println("saving file in MinIO: " + originalFileName);
                    // загрузка файла в MinIO. тоже типа препэйр
                    ByteArrayInputStream uploadStream = new ByteArrayInputStream(fileBytes);
                    minioService.uploadFile(tempFileName, uploadStream, fileSize, contentType);

                } catch (Exception e) {
                    System.out.println("exception during prepare phase: " + e.getMessage());
                    // похоже, важно кидать именно рантайм исключения (анчекд)
                    throw new RuntimeException("Prepare phase failed: " + e.getMessage(), e);
                }
            }

            // подтверждение сохранения файла. типа коммит...
            for (String key : tempToPerm.keySet()) {
                try {
                    System.out.println("commiting file: " + key);
                    minioService.commitFile(key, tempToPerm.get(key));
                } catch (Exception e) {
                    System.out.println("exception during commit phase: " + e.getMessage());
                    throw new RuntimeException("Commit phase failed: " + tempToPerm.get(key), e);
                }
            }

            userTransaction.commit();

        } catch (Exception e) {
            dragonsCount = 0;
            filesToUpload.clear();

            // откат транзакции при ошибке (и бд, и фх)
            try {
                userTransaction.rollback();
            } catch (Exception rollbackEx) {
                rollbackEx.printStackTrace();
            }

            tempToPerm.keySet().forEach(key -> {
                try {
                    minioService.deleteFile(key);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            importHistoryUnit.setStatus(ImportStatus.FAILURE);
            throw e;

        } finally {
            // обновление записи в журнале
            if (importHistoryUnit.getStatus() != ImportStatus.FAILURE) {
                importHistoryUnit.setStatus(ImportStatus.SUCCESS);
            }
            importHistoryUnit.setRowsAdded(dragonsCount);
            importHistoryUnit.setFiles(filesToUpload);
            importHistoryRepository.update(importHistoryUnit);
        }
    }

    public List<ImportHistoryUnitDTO> getImportHistory(User user, int page, int pageSize, String filterValue, String filterCol, String sortBy, String sortDir) {
        List<ImportHistoryUnitDTO> importHistory;
        if (user.getRole().equals(Roles.ADMIN)) {
            importHistory = importHistoryRepository.findAll(-1, page, pageSize, filterValue, filterCol, sortBy, sortDir);
        } else {
            importHistory = importHistoryRepository.findAll(user.getId(), page, pageSize, filterValue, filterCol, sortBy, sortDir);
        }
        return importHistory;
    }

    private List<Dragon> parseDragonsFromCsv(InputStream inputStream, User user) throws Exception {
        List<Dragon> dragons = new ArrayList<>();
        // явная установка кодировки помогает убрать кракозябры из данных (кораптилось имя первого дракона)
        // гугл говорил про BOM (Byte Order Mark)
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Dragon dragon = parseCsvLine(line);
                // TODO: вероятно, глупые костыли

                dragon.setOwnerId(user.getId());
                dragon.setUpdatedBy(user.getId());

                dragon.getCoordinates().setOwnerId(user.getId());
                dragon.getCoordinates().setUpdatedBy(user.getId());

                dragon.getCave().setOwnerId(user.getId());
                dragon.getCave().setUpdatedBy(user.getId());

                dragon.getKiller().setOwnerId(user.getId());
                dragon.getKiller().setUpdatedBy(user.getId());

                dragon.getKiller().getLocation().setOwnerId(user.getId());
                dragon.getKiller().getLocation().setUpdatedBy(user.getId());

                dragon.getHead().setOwnerId(user.getId());
                dragon.getHead().setUpdatedBy(user.getId());

                dragons.add(dragon);
            }
        }
        return dragons;
    }

    private Dragon parseCsvLine(String line) {
        Function<String, Boolean> isEmptyString = s -> s.isEmpty(); // можно, наверное, добавить isBlank()

        String[] parts = line.split(";");

        boolean isKillerExist = false;
        for (int i = 6; i < 16; i++) {
            if (!isEmptyString.apply(parts[i])) {
                isKillerExist = true;
                break;
            }
        }

        Dragon dragon = new Dragon(
                parts[0],
                new Coordinates(
                        Long.parseLong(parts[1]),
                        Integer.parseInt(parts[2]),
                        Boolean.parseBoolean(parts[3])
                ),
                new DragonCave(
                        Float.parseFloat(parts[4]),
                        Boolean.parseBoolean(parts[5])
                ),
                isKillerExist ? new Person(
                        parts[6],
                        parts[7] == null || parts[7].isEmpty() ? null : Color.valueOf(parts[7]),
                        parts[8] == null || parts[8].isEmpty() ? null : Color.valueOf(parts[8]),
                        new Location(
                                Integer.parseInt(parts[9]),
                                Integer.parseInt(parts[10]),
                                Integer.parseInt(parts[11]),
                                Boolean.parseBoolean(parts[12])
                        ),
                        java.time.LocalDate.parse(parts[13]),
                        Integer.parseInt(parts[14]),
                        Boolean.parseBoolean(parts[15])
                ) : null,
                Long.parseLong(parts[16]),
                parts[17] == null || parts[17].isEmpty() ? null : parts[17],
                Long.parseLong(parts[18]),
                parts[19] == null || parts[19].isEmpty() ? null : DragonCharacter.valueOf(parts[19]),
                new DragonHead(
                        Float.parseFloat(parts[20]),
                        Double.parseDouble(parts[21]),
                        Boolean.parseBoolean(parts[22])
                ),
                Boolean.parseBoolean(parts[23])
        );
        // System.out.println(dragon.toJson());
        return dragon;
    }
}
