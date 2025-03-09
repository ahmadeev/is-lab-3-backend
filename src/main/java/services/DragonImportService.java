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

        // запись в журнале
        ImportHistoryUnit importHistoryUnit = new ImportHistoryUnit();
        importHistoryUnit.setUser(user);
        importHistoryUnit.setStatus(ImportStatus.PENDING);
        importHistoryUnit.setRowsAdded(0);

        // TODO: вне транзакции эээээ // propaganation из ejb ???
        importHistoryRepository.save(importHistoryUnit);

        try {
            userTransaction.begin();

            // обходим список метаданных файлов (кастомное)
            for (FileUploadData fileData : fileUploads) {
                String originalFileName = fileData.getFileName();
                String contentType = fileData.getContentType();
                // читаем содержимое файла в массив байтов для повторного использования потока
                byte[] fileBytes = IOUtils.toByteArray(fileData.getInputStream());
                // fix: размер считаем сами
                long fileSize = Math.max(fileData.getFileSize(), fileBytes.length);

                // генерируем временное и постоянное имена
                String tempFileName = "temp_" + UUID.randomUUID() + "_" + originalFileName;
                String permanentFileName = "import_" + UUID.randomUUID() + "_" + originalFileName;
                // fix
                tempToPerm.put(tempFileName, permanentFileName);

                List<Dragon> allDragons = new ArrayList<>();
                try {
                    // загрузка файла в MinIO. типа препэйр
                    // TODO: можно унести вниз
                    ByteArrayInputStream uploadStream = new ByteArrayInputStream(fileBytes);
                    minioService.uploadFile(tempFileName, uploadStream, fileSize, contentType);

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
                    // сохранение драконов в БД
                    dragonService.createAll(allDragons);

                } catch (Exception e) {
                    dragonsCount = 0;
/*                    // если произошла ошибка, удаляем временный файл, если он был загружен
                    tempToPerm.keySet().forEach(key -> {
                        try {
                            minioService.deleteFile(key);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });*/
                    throw new RuntimeException("Import failed: " + e.getMessage(), e);
                }
            }

            // подтверждение сохранения файла. типа коммит...
            for (String key : tempToPerm.keySet()) {
                try {
                    System.out.println("commiting file: " + key);
                    minioService.commitFile(key, tempToPerm.get(key));
                } catch (Exception e) {
                    System.out.println("exception during commit phase: " + e.getMessage());
                    dragonsCount = 0;
                    throw new RuntimeException("Commit failed: " + tempToPerm.get(key), e);
                }
            }

            userTransaction.commit();

        } catch (Exception e) {
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
            importHistoryRepository.update(importHistoryUnit);
        }
    }

/*    // двухфазный коммит (старый метод на месте, но не используется)
    @Transactional
    public void importDragonsFromCsvWith2PC(List<FileUploadData> fileUploads, User user) throws Exception {
        Map<String, String> tempToPerm = new HashMap<>();

        int dragonsCount = 0;

        // TODO: propaganation из ejb ???
        ImportHistoryUnit importHistoryUnit = new ImportHistoryUnit();
        importHistoryUnit.setUser(user);
        importHistoryUnit.setStatus(ImportStatus.PENDING);
        importHistoryUnit.setRowsAdded(0);
        importHistoryRepository.save(importHistoryUnit);

        try {
            // обходим список метаданных файлов (кастомное)
            for (FileUploadData fileData : fileUploads) {
                String originalFileName = fileData.getFileName();
                String contentType = fileData.getContentType();

                // читаем содержимое файла в массив байтов для повторного использования потока
                byte[] fileBytes = IOUtils.toByteArray(fileData.getInputStream());

                // fix: размер считаем сами
                long fileSize = Math.max(fileData.getFileSize(), fileBytes.length);

                // генерируем временное и постоянное имена
                String tempFileName = "temp_" + UUID.randomUUID() + "_" + originalFileName;
                String permanentFileName = "import_" + UUID.randomUUID() + "_" + originalFileName;

                // fix
                tempToPerm.put(tempFileName, permanentFileName);

                List<Dragon> allDragons = new ArrayList<>();
                try {
                    // фаза Prepare: Загружаем файл во временное хранилище MinIO
                    ByteArrayInputStream uploadStream = new ByteArrayInputStream(fileBytes);
                    minioService.uploadFile(tempFileName, uploadStream, fileSize, contentType);

                    System.out.println("parsing file: " + originalFileName);
                    // парсим CSV (используем новый поток, так как предыдущий уже исчерпан)
                    ByteArrayInputStream parseStream = new ByteArrayInputStream(fileBytes);
                    List<Dragon> dragons = parseDragonsFromCsv(parseStream, user);
                    allDragons.addAll(dragons);

                    System.out.println("validating file: " + originalFileName);
                    // валидация каждого объекта
                    for (Dragon dragon : allDragons) {
                        if (!dragon.isValid()) {
                            throw new Exception("Invalid dragon found in file " + originalFileName);
                        }
                    }
                    dragonsCount += allDragons.size();

                    System.out.println("saving file: " + originalFileName);
                    // сохраняем всех драконов в БД
                    dragonService.createAll(allDragons);

                } catch (Exception e) {
                    dragonsCount = 0;
                    // если произошла ошибка, удаляем временный файл, если он был загружен
                    System.out.println("exception during prepare phase: " + e.getMessage());

                    try {
                        for(String key : tempToPerm.keySet()) {
                            System.out.println("deleting file: " + key);
                            minioService.deleteFile(key);
                        }
                    } catch (Exception delEx) {
                        delEx.printStackTrace();
                    }
                    throw new RuntimeException(e);
                }
            }

            // фаза Commit: Подтверждение сохранения файла
            for(String key : tempToPerm.keySet()) {
                try {
                    System.out.println("commiting file: " + key);
                    minioService.commitFile(key, tempToPerm.get(key));
                } catch (Exception e) {
                    System.out.println("exception during commit phase: " + e.getMessage());
                    dragonsCount = 0;
                    throw new RuntimeException("Error during file commit for file: " + tempToPerm.get(key), e);
                }
            }
        } catch (Exception e) {
            importHistoryUnit.setStatus(ImportStatus.FAILURE);
            throw e;
        } finally {
            if (importHistoryUnit.getStatus() != ImportStatus.FAILURE) {
                importHistoryUnit.setStatus(ImportStatus.SUCCESS);
            }
            importHistoryUnit.setRowsAdded(dragonsCount);
            importHistoryRepository.update(importHistoryUnit);
        }
    }*/

    @Transactional
    public void importDragonsFromCsv(List<InputStream> inputStreams, User user) throws Exception {
        List<Dragon> allDragons = new ArrayList<>();
        ImportHistoryUnit importHistoryUnit = new ImportHistoryUnit();
        importHistoryUnit.setUser(user);

        try {
            // парсим и валидируем драконов из всех файлов
            for (InputStream inputStream : inputStreams) {
                List<Dragon> dragons = parseDragonsFromCsv(inputStream, user);
                allDragons.addAll(dragons);
            }

            // проверка валидности всех драконов
            for (Dragon dragon : allDragons) {
                if (!dragon.isValid()) {
                    System.out.println("dragon: %b\ncoordinates: %b\ncave: %b\nkiller: %b\nlocation: %b\nhead: %b".formatted(
                            dragon.isValid(),
                            dragon.getCoordinates().isValid(),
                            dragon.getCave().isValid(),
                            dragon.getKiller().isValid(),
                            dragon.getKiller().getLocation().isValid(),
                            dragon.getHead().isValid()
                    ));
                    throw new Exception("Invalid dragon found");
                }
            }

            // сохраняем всех драконов в транзакции (если какой-то из драконов невалидный, до этой строки не доходит)
            dragonService.createAll(allDragons);

            // логируем успешный импорт
            importHistoryUnit.setStatus(ImportStatus.SUCCESS);
            importHistoryUnit.setRowsAdded(allDragons.size());
        } catch (Exception e) {
            // логируем ошибку
            importHistoryUnit.setStatus(ImportStatus.FAILURE);
            importHistoryUnit.setRowsAdded(0);
            throw e;
        } finally {
            importHistoryRepository.save(importHistoryUnit);
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
