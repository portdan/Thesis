package Utils;

import java.util.List;

import org.apache.log4j.Logger;

import PlannerAndLearner.ModelSearchNode;

public class MCTS {

	private final static Logger LOGGER = Logger.getLogger(MCTS.class);

	ModelSearchNode root;
	double C;

	public MCTS( ModelSearchNode root, double C) {
		this.root = root;
		this.C = C;
	}

	public ModelSearchNode selectBestNode(ModelSearchNode node) {

		double n = node.getNumOfVisits();

		if(node.getNumOfVisits() == 0)
			return node;
		
		if(node.getChildren() == null || node.getChildren().isEmpty())
			return node;

		ModelSearchNode maxScoreChild = node.getChildren().get(0);
		double maxScore = 0;

		for (ModelSearchNode child : node.getChildren()) {

			double cn = child.getNumOfVisits();
			double v = child.getValue();
			double score = 0;

			if(cn == 0)
				score = Double.POSITIVE_INFINITY;
			else
				score = v/cn + C*Math.sqrt(Math.log(n)/cn);

			if(score > maxScore) {
				maxScore = score;
				maxScoreChild = child;
			}
		}

		return selectBestNode(maxScoreChild);
	}

	public void backpropogateNode(ModelSearchNode node, double value, int visit) {

		node.setNumOfVisits(node.getNumOfVisits() + visit);
		node.setValue(node.getValue() + value);

		if(!node.equals(root))
			backpropogateNode(node.getParent(), value, visit);
		//backpropogateNode(node.getParent(), calcValueAverage(node.getParent()));
	}

	private double calcValueAverage(ModelSearchNode node) {

		double sum = 0;
		int count = 0;

		for (ModelSearchNode child : node.getChildren()) {
			if(child.getNumOfVisits()>0) {
				sum+=child.getValue();
				count++;
			}
		}

		if(count==0)
			return 0;
		else
			return sum/count;
	}

	public void removeNode(ModelSearchNode searchNode) {
		
		if(searchNode.equals(root))
			return;

		ModelSearchNode parent = searchNode.getParent();

		parent.getChildren().remove(searchNode);

		backpropogateNode(parent, -searchNode.getValue(), -searchNode.getNumOfVisits()); 
		//backpropogateNode(parent, calcValueAverage(parent)); 

		searchNode = null;
		
		if(parent.getChildren().isEmpty())
			removeNode(parent);

	}
}
