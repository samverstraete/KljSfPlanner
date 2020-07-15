package ui.importer;

import com.google.inject.Inject;
import com.google.inject.Injector;
import domain.importing.WizardData;
import javafx.beans.binding.When;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/*
Source: http://bekwam.blogspot.com/2016/04/a-wizard-sequence-of-forms-in-javafx.html
 */
public class WizardImportController {
	@FXML
	GridPane contentPanel;

	@FXML
	Button btnNext, btnBack, btnCancel;

	@FXML
	Label title, subtitle;

	@Inject
	Injector injector;

	@Inject
	WizardData model;

	private final List<Parent> steps = new ArrayList<>();
	private final IntegerProperty currentStep = new SimpleIntegerProperty(-1);
	private final String CONTROLLER_KEY = "controller";
	protected Consumer<WizardData> dataCallback ;

	public void setTextCallback(Consumer<WizardData> dataCallback) {
		this.dataCallback = dataCallback ;
	}

	@FXML
	public void BackAction(ActionEvent actionEvent) {
		if( currentStep.get() > 0 ) {
			contentPanel.getChildren().remove( steps.get(currentStep.get()) );
			currentStep.set( currentStep.get() - 1 );
			((WizardImportController)steps.get(currentStep.get()).getProperties().get(CONTROLLER_KEY)).activate();
			contentPanel.getChildren().add( steps.get(currentStep.get()) );
		}
	}

	@FXML
	public void NextAction(ActionEvent actionEvent) {
		Parent p = steps.get(currentStep.get());
		Object controller = p.getProperties().get(CONTROLLER_KEY);

		// validate
		Method v = getMethod( Validate.class, controller );
		if( v != null ) {
			try {
				Object retval = v.invoke(controller);
				if( retval != null && !((Boolean) retval)) {
					return;
				}

			} catch (IllegalAccessException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}

		// submit
		Method sub = getMethod( Submit.class, controller );
		if( sub != null ) {
			try {
				sub.invoke(controller);
			} catch (IllegalAccessException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}

		if( currentStep.get() < (steps.size()-1) ) {
			contentPanel.getChildren().remove( steps.get(currentStep.get()) );
			currentStep.set( currentStep.get() + 1 );
			((WizardImportController)steps.get(currentStep.get()).getProperties().get(CONTROLLER_KEY)).activate();
			contentPanel.getChildren().add( steps.get(currentStep.get()) );
		}
	}

	@FXML
	public void CancelAction(ActionEvent actionEvent) {
		Stage stage = (Stage) btnCancel.getScene().getWindow();
		stage.close();
	}

	@FXML
	public void initialize() throws Exception {
		final JavaFXBuilderFactory bf = new JavaFXBuilderFactory();

		final Callback<Class<?>, Object> cb = (clazz) -> injector.getInstance(clazz);

		for(String resource : new String[]{ "/ui/WizardImportColumns.fxml","/ui/WizardImportSportfeest.fxml",
				"/ui/WizardImportReeksen.fxml","/ui/WizardImportRingen.fxml"}) {
			FXMLLoader fxmlLoaderStep = new FXMLLoader(WizardImportController.class.getResource(resource), null, bf, cb);
			Parent step = fxmlLoaderStep.load();
			step.getProperties().put(CONTROLLER_KEY, fxmlLoaderStep.getController());
			steps.add(step);
		}

		btnBack.disableProperty().bind( currentStep.lessThanOrEqualTo(0) );
		btnNext.textProperty().bind(
				new When( currentStep.lessThan(steps.size()-1) )
						.then("Volgende >")
						.otherwise("Voltooien")
		);

		currentStep.set( 0 );  // first element
		contentPanel.getChildren().add( steps.get( currentStep.get() ));
		((WizardImportController)steps.get(currentStep.get()).getProperties().get(CONTROLLER_KEY)).activate();

		title.textProperty().bindBidirectional( model.titleProperty() );
		subtitle.textProperty().bindBidirectional( model.subtitleProperty() );
	}

	private Method getMethod(Class<? extends Annotation> an, Object obj) {

		if( an == null ) {
			return null;
		}

		if( obj == null ) {
			return null;
		}

		Method[] methods = obj.getClass().getMethods();
		if(methods.length > 0) {
			for( Method m : methods ) {
				if( m.isAnnotationPresent(an)) {
					return m;
				}
			}
		}
		return null;
	}

	public void setFilename(String filename) {
		model.setFilename(filename);
		((WizardImportColumnsController)steps.get(0).getProperties().get(CONTROLLER_KEY)).loadFile(filename);
	}

	public void activate(){	}
}
