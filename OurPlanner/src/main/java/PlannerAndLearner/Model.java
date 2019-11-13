package PlannerAndLearner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import OurPlanner.Globals;

public class Model {

	Map<String,Action> actions;
	String startOfModel;

	private boolean firstAction;

	public Model(Model model) {
		this.actions = new LinkedHashMap<String,Action>();

		for (Entry<String, Action> pair : model.actions.entrySet())
			actions.put(pair.getKey(),new Action(pair.getValue()));

		this.startOfModel = model.startOfModel;
		this.firstAction = model.firstAction;
	}

	public Model() {

		actions = new LinkedHashMap<String,Action>();
		startOfModel = "";

		firstAction = false;
	}

	public boolean readModel(String path){

		actions.clear();

		List<String> content = null;

		try {
			content = Files.readAllLines(Paths.get(path));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		for (int i = 0; i < content.size(); i++) {

			String line  = content.get(i);

			if(line.startsWith("(:action ")) {

				firstAction = true;

				i = extractAction(content, i);
			}
			else if(!firstAction)
				startOfModel+=line + '\n';
		}

		return true;
	}

	public Model extendModel(TempModel tempModel){

		Model res = new Model(this);

		for (TempAction tempAct : tempModel.tempActions) {

			Action act = res.actions.get(tempAct.name);

			act.preconditions.removeAll(tempAct.preconditionsSub);
			act.effects.removeAll(tempAct.effectsSub);

			act.preconditions.addAll(tempAct.preconditionsAdd);
			act.effects.addAll(tempAct.effectsAdd);

		}

		return res;
	}

	private int extractAction(List<String> content, int actionStartLineNumber) {

		String line = "";
		int ind = -1;

		Action action = new Action();

		ind = actionStartLineNumber;
		line  = content.get(ind);

		String actionName  = line.substring(9);

		if(actionName.contains(Globals.PARAMETER_INDICATION)) {
			actionName = actionName.replace(Globals.PARAMETER_INDICATION, " ");
			action.isParam = true;
		}

		action.name = actionName;

		ind+=1;
		line = content.get(ind);	

		action.agentLine = line;

		String agentName  = (line.split("-")[1]).trim();
		action.agent = agentName;

		ind+=1;
		line = content.get(ind);	
		action.parametersLine = line;

		ind+=1;

		while(true) {

			ind++;
			line = content.get(ind);

			if(line.startsWith("\t:effect")) {
				break;
			}
			else if (line.startsWith("\t)")) {
				ind+=1;
				break;
			}
			else {

				line=line.trim();
				
				if(!line.equals("()"))
					action.preconditions.add(line);
			}
		}

		while(true) {

			ind++;
			line = content.get(ind);

			if (line.startsWith("\t)") || line.startsWith(")")) {
				break;
			}
			else	{	
				line=line.trim();
				action.effects.add(line);
			}
		}

		actions.put(actionName,action);

		return ind;
	}

	public String reconstructModelString() {

		String res = startOfModel;

		for (Action act: actions.values()) {

			String actionName = act.name;

			if(act.isParam)
				actionName = act.name.replace(" ",Globals.PARAMETER_INDICATION);

			res+= "(:action " + actionName + '\n';
			res+=  act.agentLine + '\n';
			res+=  act.parametersLine + '\n';

			if(act.preconditions.size()>1)
				res+=  "\t:precondition (and" + '\n';
			else if(act.preconditions.size()==1)
				res+=  "\t:precondition " + '\n';
			else
				res+=  "\t:precondition ()" + '\n';

			for (String pre : act.preconditions)
				res += "\t\t" + pre + '\n';

			if(act.preconditions.size()>1)
				res += "\t)\n";

			if(act.effects.size()>1)
				res+= "\t:effect (and" + '\n';
			else if(act.effects.size()==1)
				res+=  "\t:effect " + '\n';
			else
				res+=  "\t:effect ()" + '\n';

			for (String eff : act.effects)
				res += "\t\t" + eff + '\n';

			res += "\t)\n)\n";
		}

		res += ')';

		return res;

	}

}
