package testcorrectness.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IEditorPart;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import java.util.Scanner;
import java.util.Stack;
import java.util.regex.Pattern;

public class TestCorrectnessHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		// Initializing the display message as an empty string.
		String displayText = new String("");

		// Source code of the file currently opened in the editor.
		IEditorPart activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		ITextEditor textEditor = (ITextEditor)activeEditor;
		IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
		String sourcecode = document.get();
		IFile file = ((FileEditorInput) activeEditor.getEditorInput()).getFile();

		// Remove the existing warnings respective to test methods without assertions. 
		try {
			for (IMarker marker : (file).findMarkers(IMarker.PROBLEM, true, 1)) {
				if ((((String) marker.getAttribute(IMarker.MESSAGE)).startsWith("Test method"))) {
					marker.delete();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}


		Scanner scanner = new Scanner(sourcecode);

		int testCount = 0;
		int assertionPresentTestCount = 0;
		int lineNumber = 1;
		int testLineNumber = 1;

		while (scanner.hasNextLine()) {

			boolean flag = false;
			String line = scanner.nextLine();

			Pattern testPattern = Pattern.compile("^([^//]*)(@Test)", Pattern.MULTILINE);
			Pattern assertionPattern = Pattern.compile("^([^//]*)[a-zA-Z]*[\\.]?(assert)[a-zA-Z]*[\\s]*[\\(]?", Pattern.MULTILINE);
			Pattern testEndPattern = Pattern.compile("^([^//]*)[^\\w][\\}]$", Pattern.MULTILINE);

			Pattern conditionalStatementPattern = Pattern.compile("^([^//]*)(if|while|switch|for)[\\s]*[\\(]", Pattern.MULTILINE);


			boolean ifTestPresent = testPattern.matcher(line).find();

			if(ifTestPresent) {
				testCount+=1;
				Stack<String> openBraces = new Stack<>();
				testLineNumber = lineNumber;
				line = scanner.nextLine();
				lineNumber++;
				do {

					if(assertionPattern.matcher(line).find()) {
						assertionPresentTestCount+=1;
						flag = true;
						break;
					}else if (conditionalStatementPattern.matcher(line).find()) {
						openBraces.push("{");
					}
					line = scanner.nextLine();
					lineNumber++;

					while( (!(openBraces.empty())) && (testEndPattern.matcher(line).find()) ) {
						openBraces.pop();
						line = scanner.nextLine();
						lineNumber++;
					}

				}while(!(testEndPattern.matcher(line).find()));

				if(!flag) {

					// Add marker to the test methods without assertions.
					try {
						IMarker marker = (file).createMarker(IMarker.PROBLEM);
						marker.setAttribute(IMarker.LINE_NUMBER, testLineNumber);
						marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
						marker.setAttribute(IMarker.MESSAGE,
								"Test method has no assert statements");
						marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
					} catch (Exception e) {
						e.printStackTrace();
					}

				}


			}

			lineNumber++;


		}

		scanner.close();

		if(testCount!=0) {
			float specifiedAssertion = (float) ((assertionPresentTestCount*100)/testCount);
			

			displayText="Number of tests = " + testCount + "\n" + "Number of tests with assert statements = " + assertionPresentTestCount
					+ "\n" + "Ratio of test method with assertions = " + specifiedAssertion + "%" ;
		}else {
			displayText = "There are no tests in the test suite";
		}
		
		

		MessageDialog.openInformation(window.getShell(), "Test Correctness Outcome", displayText);


		return null;
	}
}
