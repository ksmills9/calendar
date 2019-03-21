import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class GUIPrompt extends Application {
    private Account userAccount;
    private SceneController sceneCtrl;
    
    //credit: https://stackoverflow.com/questions/10121991/javafx-application-icon
	//change icon thumbnail of program to ---.png work in progress
	//primaryStage.getIcons().add(new Image("file:------"));
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setMaximized(true);
        primaryStage.setTitle("Planner");
        sceneCtrl = new SceneController();
        primaryStage.setScene(sceneCtrl.getMain());
        primaryStage.show();
        if (!sceneCtrl.getConn().connected()) {
            connError();
            primaryStage.close();
        }
    }

    void connError(){
        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
    	errorAlert.setHeaderText("Connection error");
    	errorAlert.setContentText("Could not connect to the database");
        errorAlert.showAndWait();
    }
}
