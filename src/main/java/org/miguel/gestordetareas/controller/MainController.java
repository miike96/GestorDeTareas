package org.miguel.gestordetareas.controller;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.miguel.gestordetareas.dao.TaskDAO;
import org.miguel.gestordetareas.model.Task;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Optional;

public class MainController {

    @FXML
    private TextField titleField;

    @FXML
    private TextField descriptionField;

    @FXML
    private TableView<Task> taskTable;

    @FXML
    private TableColumn<Task, String> colTitle;

    @FXML
    private TableColumn<Task, String> colDescription;

    @FXML
    private TableColumn<Task, Boolean> colCompleted;

    @FXML
    private TableColumn<Task, Void> colActions;

    private TaskDAO taskDAO;
    private ObservableList<Task> taskList;

    @FXML
    public void initialize() {
        taskDAO = new TaskDAO();
        taskList = FXCollections.observableArrayList(taskDAO.getAllTasks());

        taskTable.setEditable(true);
        colCompleted.setEditable(true);

        colTitle.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTitle()));
        colDescription.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));

        colCompleted.setCellValueFactory(cellData -> {
            Task task = cellData.getValue();
            SimpleBooleanProperty completedProperty = new SimpleBooleanProperty(task.isCompleted());

            completedProperty.addListener((obs, oldVal, newVal) -> {
                task.setCompleted(newVal);
                taskDAO.updateTask(task);
            });

            return completedProperty;
        });
        colCompleted.setCellFactory(CheckBoxTableCell.forTableColumn(colCompleted));

        colActions.setCellFactory(getActionCellFactory());

        taskTable.setItems(taskList);
    }

    private Callback<TableColumn<Task, Void>, TableCell<Task, Void>> getActionCellFactory() {
        return param -> new TableCell<>() {
            private final Button btnEdit = new Button("Editar");
            private final Button btnDelete = new Button("Borrar");
            private final HBox pane = new HBox(10, btnEdit, btnDelete);

            {
                btnEdit.setOnAction(event -> {
                    Task task = getTableView().getItems().get(getIndex());
                    showEditDialog(task);
                });

                btnDelete.setOnAction(event -> {
                    Task task = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirmar borrado");
                    confirm.setHeaderText("¿Quieres borrar la tarea \"" + task.getTitle() + "\"?");
                    Optional<ButtonType> result = confirm.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        taskDAO.deleteTask(task.getId());
                        taskList.remove(task);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        };
    }

    private void showEditDialog(Task task) {
        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("Editar tarea");
        dialog.setHeaderText("Editar título y descripción");

        ButtonType okButtonType = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        TextField titleInput = new TextField(task.getTitle());
        TextArea descriptionInput = new TextArea(task.getDescription());
        descriptionInput.setPrefRowCount(4);

        VBox content = new VBox(10);
        content.getChildren().addAll(new Label("Título:"), titleInput, new Label("Descripción:"), descriptionInput);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                String newTitle = titleInput.getText().trim();
                String newDesc = descriptionInput.getText().trim();
                if (newTitle.isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "El título no puede estar vacío.", ButtonType.OK);
                    alert.showAndWait();
                    return null;
                }
                task.setTitle(newTitle);
                task.setDescription(newDesc);
                return task;
            }
            return null;
        });

        Optional<Task> result = dialog.showAndWait();
        result.ifPresent(t -> {
            taskDAO.updateTask(t);
            refreshTable();
        });
    }

    @FXML
    private void onAddClicked() {
        String title = titleField.getText();
        String description = descriptionField.getText();

        if (title == null || title.trim().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "El título es obligatorio.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        if (description == null || description.trim().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "La descripción es obligatoria.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        boolean duplicate = taskList.stream()
                .anyMatch(task -> task.getTitle().equalsIgnoreCase(title.trim()));

        if (duplicate) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Ya existe una tarea con ese título.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        Task newTask = new Task(0, title.trim(), description.trim(), false);
        taskDAO.addTask(newTask);

        refreshTable();

        titleField.clear();
        descriptionField.clear();
    }

    private void refreshTable() {
        taskList.clear();
        taskList.addAll(taskDAO.getAllTasks());
    }

    @FXML
    private void onExportExcel() {
        if (taskList.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "No hay tareas para exportar.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar archivo Excel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo Excel", "*.xlsx"));
        File file = fileChooser.showSaveDialog(taskTable.getScene().getWindow());

        if (file != null) {
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Tareas");

                // Encabezados
                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("ID");
                header.createCell(1).setCellValue("Título");
                header.createCell(2).setCellValue("Descripción");
                header.createCell(3).setCellValue("Completada");

                // Filas de datos
                int rowNum = 1;
                for (Task task : taskList) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(task.getId());
                    row.createCell(1).setCellValue(task.getTitle());
                    row.createCell(2).setCellValue(task.getDescription());
                    row.createCell(3).setCellValue(task.isCompleted() ? "Sí" : "No");
                }

                // Autoajustar columnas
                for (int i = 0; i < 4; i++) {
                    sheet.autoSizeColumn(i);
                }

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    workbook.write(fos);
                }

                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Exportado correctamente a " + file.getAbsolutePath(), ButtonType.OK);
                alert.showAndWait();

            } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR, "Error al exportar: " + e.getMessage(), ButtonType.OK);
                alert.showAndWait();
            }
        }
    }
}
