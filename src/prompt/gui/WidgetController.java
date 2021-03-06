package src.prompt.gui;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import src.Event;
import src.userAlarm;

import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TimeZone;

/**
 * WidgetController manages different utility UI windows
 */
public class WidgetController extends Controller {
    private Stage widget = new Stage();
    private Scene widgetScene = new Scene(new Pane());
    private MainUIController mainUICtrl;

    /**
     * Create the WidgetController with a reference back to the MainUIController it was created from
     * @param mainUICtrl reference to MainUIController
     */
    WidgetController(MainUIController mainUICtrl){
        this.mainUICtrl = mainUICtrl;
    }

    @FXML
    TextField eventName;
    @FXML
    TextArea eventDescription;
    @FXML
    TextField eventLocation;
    @FXML
    DatePicker eventStartDate;
    @FXML
    DatePicker eventEndDate;
    @FXML
    TextField eventStartTime;
    @FXML
    TextField eventEndTime;
    @FXML
    ComboBox frequencyCombo;
    @FXML
    TextField reminderName;
    @FXML
    DatePicker reminderDate;
    @FXML 
    TextField reminderTime;
    @FXML
    Label acc_name;
    @FXML
    Label acc_id;
    @FXML
    Label acc_timezone;
    @FXML
    Button theme_toggle;
    @FXML
    HBox change_box;
    @FXML
    ComboBox<String> hoursCombo;
    @FXML
    ComboBox<String> minutesCombo;
    @FXML
    ComboBox<String> amPmCombo;
    @FXML
    ToggleButton repeatToggleBtn;
    @FXML
    RadioButton monRadio;
    @FXML
    RadioButton tueRadio;
    @FXML
    RadioButton wedRadio;
    @FXML
    RadioButton thurRadio;
    @FXML
    RadioButton friRadio;
    @FXML
    RadioButton satRadio;
    @FXML
    RadioButton sunRadio;
    @FXML
    TextField alarmNameTxt;
    @FXML
    VBox alarmsVBox;

    /**
     * Initialize the class by creating a stage to display all the widgets
     */
    @Override
    public void initializeClass() {
        widget.setWidth(500);
        widget.setAlwaysOnTop(true);
        widget.setScene(widgetScene);
        widgetScene.getStylesheets().add("/assets/stylesheet.css");
        widget.initStyle(StageStyle.UNDECORATED);
        widget.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) closeWidget();
        });
    }
    
    public void alarmIni() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource( "/assets/templates/CreateAlarmWidget.fxml" ));
            loader.setController(this);
            widgetScene.setRoot(loader.load());
        } catch (Exception ex){ex.printStackTrace();}
        widget.setTitle("Create an Event");
    	//hour Combo
    	hoursCombo.getItems().addAll("01","02","03","04","05","06","07","08","09","10","11","12");

    	//minutes Combo
    	minutesCombo.getItems().addAll("00","01","02","03","04","05","06","07","08","09","10",
    			"11","12","13","14","15","16","17","18","19","20",
    			"21","22","23","24","25","26","27","28","29","30",
    			"31","32","33","34","35","36","37","38","39","40",
    			"41","42","43","44","45","46","47","48","49","50",
    			"51","52","53","54","55","56","57","58","59");
    	//amPm Combo
    	amPmCombo.getItems().addAll("AM","PM");
    	showWidget();
    }

    /**
     * Load the event and reminder creation UI onto the widget window and display it
     */
    @SuppressWarnings("unchecked")
    public void loadEventCreation(){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource( "/assets/templates/CreateEventWidget.fxml" ));
            loader.setController(this);
            widgetScene.setRoot(loader.load());
        } catch (Exception ex){ex.printStackTrace();}
        widget.setTitle("Create an Event");
        frequencyCombo.getItems().addAll(Arrays.asList(Event.freqArray));
        frequencyCombo.getSelectionModel().select(0);
        showWidget();
    }

    /**
     * Takes the input from the fields of the Create Event window and calls necessary methods to create the event and display
     * error messages in case of invalid input
     */
    public void createEvent(){
        String event_name = eventName.getText();
        String event_desc = "";
        event_desc += eventDescription.getText();
        String event_location = "";
        event_location += eventLocation.getText();
        String event_freq = frequencyCombo.getSelectionModel().getSelectedItem().toString();
        String event_start = eventStartDate.getValue() + " " + eventStartTime.getText();
        String event_end = eventEndDate.getValue() + " " + eventEndTime.getText();
        Event newEvent = new Event(0, getSceneCtrl().getUserAccount().getID(), event_name, event_desc,
                event_start, event_end, event_location, event_freq);
        if (newEvent.isValidInterval()) {
            int event_ID = getSceneCtrl().getConn().addEvent(newEvent);
            newEvent.setID(event_ID);
            getSceneCtrl().getUserAccount().getAllEvents().addEvent(newEvent);
            errorBox("Event Created Successfully", "", Alert.AlertType.CONFIRMATION);
            mainUICtrl.CreateCalendar();
        } else errorBox("Invalid Start and End Time", "Start time must be set earlier than end time", Alert.AlertType.ERROR);
        closeWidget();
    }
    
    /**
     * loads the alarm widget
     */
    public void loadAlarms() {
    	try {
    		FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/templates/CreateAlarmWidget.fxml"));
    		loader.setController(this);
    		widgetScene.setRoot(loader.load());
    	}catch(Exception ex) {ex.printStackTrace();}
    	widget.setTitle("Manage Alarms");
    	widget.setWidth(600);

    	//loads the user's alarms into the widget
    	VBox root = new VBox();
    	root.setFillWidth(true);
    	int accID = getSceneCtrl().getUserAccount().getID();
    	ArrayList<userAlarm> alarmList = new ArrayList<userAlarm>();
    	alarmList=getSceneCtrl().getConn().loadAlarmsFromDB(accID);
    	for(userAlarm alarm: alarmList) {
    		root.getChildren().add(generateAlarmGrid(alarm));
    	}
    	ScrollPane container = new ScrollPane(root);
    	container.setFitToHeight(true);
        container.setFitToWidth(true);
        alarmsVBox.getChildren().add(container);

    	//hour Combo
    	hoursCombo.getItems().addAll("01","02","03","04","05","06","07","08","09","10","11","12");

    	//minutes Combo
    	minutesCombo.getItems().addAll("00","01","02","03","04","05","06","07","08","09","10",
    			"11","12","13","14","15","16","17","18","19","20",
    			"21","22","23","24","25","26","27","28","29","30",
    			"31","32","33","34","35","36","37","38","39","40",
    			"41","42","43","44","45","46","47","48","49","50",
    			"51","52","53","54","55","56","57","58","59");
    	//amPm Combo
    	amPmCombo.getItems().addAll("AM","PM");
    	showWidget();
    }
    
    public void createAlarm() {
    	String aHour = hoursCombo.getValue();
    	String aMin = minutesCombo.getValue();
    	String amOrPm = amPmCombo.getValue();
    	String alName = alarmNameTxt.getText();
    	Boolean shouldRepeat;
    	if (repeatToggleBtn.isSelected()) {
    		shouldRepeat = true;
    	}
    	else {
    		shouldRepeat = false;
    	}
    	ArrayList<String> daysRepeating  = new ArrayList<String>();
    	if(monRadio.isSelected()) {
    		daysRepeating.add("Mon");
    	}
    	if(tueRadio.isSelected()) {
    		daysRepeating.add("Tue");
    	}
    	if(wedRadio.isSelected()) {
    		daysRepeating.add("Wed");
    	}
    	if(thurRadio.isSelected()) {
    		daysRepeating.add("Thu");
    	}
    	if(friRadio.isSelected()) {
    		daysRepeating.add("Fri");
    	}
    	if(satRadio.isSelected()) {
    		daysRepeating.add("Sat");
    	}
    	if(sunRadio.isSelected()) {
    		daysRepeating.add("Sun");
    	}
    	int accID = getSceneCtrl().getUserAccount().getID();
    	getSceneCtrl().getConn().addAlarm(accID,alName,aHour,aMin,amOrPm,shouldRepeat,daysRepeating);
    	String time="";
    	if(amOrPm.equalsIgnoreCase("pm")) {
    		time = String.valueOf(Integer.valueOf(aHour)+12);
    	}
    	else {
    		time = aHour;
    	}
    	time+=":"+aMin;
    	String days="";
    	for(int i=0; i<daysRepeating.size();i++) {
    		days+=daysRepeating.get(i);
    		if(i!=daysRepeating.size()-1) {
    			days+=",";
    		}
    	}
    	userAlarm al = new userAlarm(alName, time, days,1);
    	MainUIController.activateAlarm(al);
    	errorBox("Alarm Created Successfully", "", Alert.AlertType.CONFIRMATION);

    }
    
    public void createReminder(){
        
    }

    /**
     * Close the widget window and remove the overlay from the main window
     */
    public void closeWidget(){
        widget.close();
        mainUICtrl.hideOverlay();
    }

    /**
     * Display the widget window and cast an overlay on the main window
     */
    public void showWidget(){
        if(widget.isShowing()) widget.close();
        widget.show();
        mainUICtrl.showOverlay();
    }

    /**
     * Display information about all events on a particular date when double clicked
     * @param onThisDay List of all events to show
     */
    public void loadEventsOnDate(ArrayList<Event> onThisDay){
        VBox root = new VBox();
        root.getStylesheets().add("/assets/stylesheet.css");
        root.setFillWidth(true);
        for(Event event: onThisDay){
            root.getChildren().add(generateEventGrid(event));
        }
        widgetScene.setRoot(root);
        showWidget();
    }

    public static GridPane generateEventGrid(Event event){
        GridPane layout = new GridPane();
        layout.getStyleClass().add("event-grid");
        Label eventName = new Label(event.getName());
        eventName.getStyleClass().add("event-name");
        Label description = new Label(event.getDescription());
        Label location = new Label(" at " + event.getLocation());
        Label startTime = new Label("From: " + event.getStartTimeString());
        Label endTime = new Label("To: " + event.getEndTimeString());
        startTime.getStyleClass().add("event-time");
        endTime.getStyleClass().add("event-time");
        layout.addRow(0, eventName, location);
        layout.add(description, 0, 1, 2, 1);
        layout.add(startTime, 0, 2, 2, 1);
        layout.add(endTime, 0, 3, 2, 1);
        return layout;
    }

    public static GridPane generateAlarmGrid(userAlarm alarm) {
    	GridPane layout = new GridPane();
    	layout.getStyleClass().add("event-grid");
    	Label alarmName = new Label(alarm.getAName());
    	alarmName.getStyleClass().add("event-name");
    	Label alarmTime = new Label("Time: "+alarm.getATime());
    	Label repeatDays = new Label("Repeat: "+alarm.getDays());
    	Button delAlarm = new Button("Delete Alarm");

    	alarmTime.getStyleClass().add("event-time");
    	repeatDays.getStyleClass().add("event-time");
    	layout.addRow(0, alarmName);
    	layout.addRow(1, alarmTime);
    	layout.addRow(2, repeatDays);
    	layout.addRow(3, delAlarm);

    	return layout;
    }

    //delete button handler


    public void displayUserPanel(){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource( "/assets/templates/profileMenu.fxml" ));
            loader.setController(this);
            widgetScene.setRoot(loader.load());
        } catch (Exception ex){ex.printStackTrace();}
        widget.setTitle("My account");
        acc_id.setText(""+getSceneCtrl().getUserAccount().getID());
        acc_name.setText(getSceneCtrl().getUserAccount().getName());
        acc_timezone.setText(getSceneCtrl().getUserAccount().getTimeZone());
        theme_toggle.setText(!(getSceneCtrl().getIsDark()) ? "Dark theme" : "Light theme");
        showWidget();
    }

    public void changeCSS(){
        getSceneCtrl().toggleStyleSheet();
        theme_toggle.setText(!(getSceneCtrl().getIsDark()) ? "Dark theme" : "Light theme");
    }

    public void logoutclicked(){
        closeWidget();
        getSceneCtrl().logoutUser();
    }

    @SuppressWarnings("unchecked")
    public void changeTMZSub(){
        Label label = new Label("New Timezone: ");
        ArrayList<String> tzList = new ArrayList(Arrays.asList(TimeZone.getAvailableIDs()));
        ComboBox timeZoneCombo = new ComboBox();
        timeZoneCombo.getItems().setAll(tzList);
        int x = tzList.indexOf(TimeZone.getDefault().getID());
        timeZoneCombo.getSelectionModel().select(x);
        Button btn = new Button("Change");
        btn.getStyleClass().add("btn-primary");
        btn.setOnMouseClicked(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent mouseEvent) {
                change_box.getChildren().clear();
                String newtmz = timeZoneCombo.getSelectionModel().getSelectedItem().toString();
                try {
                    getSceneCtrl().getConn().changeUser(getSceneCtrl().getUserAccount().getName(),
                            newtmz, "account_timezone");
                    errorBox("Time Zone Changed", "Timezone changed to " + newtmz, Alert.AlertType.CONFIRMATION);
                } catch (SQLException ex) {ex.printStackTrace();}
            }
        });
        change_box.getChildren().addAll(label, timeZoneCombo, btn);
    }

    public void changePassSub(){
        VBox root = new VBox();
        Button btn = new Button("Change");
        HBox oldpassbox = new HBox();
        HBox newpassbox = new HBox();
        HBox retypebox = new HBox();
        Label oldpassprompt = new Label("Enter old password: ");
        Label newpassprompt = new Label("Enter new password: ");
        Label retypePrompt = new Label("Retype new password: ");
        PasswordField oldpass = new PasswordField();
        PasswordField newpass = new PasswordField();
        PasswordField retypepass = new PasswordField();
        oldpassbox.getChildren().addAll(oldpassprompt, oldpass);
        newpassbox.getChildren().addAll(newpassprompt, newpass);
        retypebox.getChildren().addAll(retypePrompt, retypepass);
        root.getChildren().addAll(oldpassbox, newpassbox, retypebox, btn);
        change_box.getChildren().add(root);
        btn.setOnMouseClicked(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent mouseEvent) {
                change_box.getChildren().clear();
                try {
                    if(!newpass.getText().equals(retypepass.getText())){
                        errorBox("Try again!", "Passwords do not match", Alert.AlertType.ERROR);
                    }
                    else if(getSceneCtrl().getConn().changeUserPass(getSceneCtrl().getUserAccount().getName(),
                            oldpass.getText(), newpass.getText())) {
                        errorBox("Password changed successfully", "", Alert.AlertType.CONFIRMATION);
                    }
                } catch (SQLException ex){
                    errorBox("An error occured", "Please try again", Alert.AlertType.ERROR);
                }
            }
        });
    }
}
