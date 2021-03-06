package commandline;

import java.util.ArrayList;
import java.util.Scanner;

public class TTController {
	
	private TTModel model;
	private TTView view;

	private boolean isOnline;
	private boolean isPaused;

	private Scanner s = new Scanner(System.in);
	public TTController(TTModel model, TTView view) {
		this.model = model;
		this.view = view;
	}

//second constructor, for JUnit testing-only
	public TTController(TTModel model, TTView view, String test){
			this.model = model;
			this.view = view;
			//does not stat the game
		}

//		startGame();
	
	public void startGame() {
		while(!model.isEnd()) {
			model.setRoundNumber(model.getRoundNumber()+1);
			checkHumanPlayerEliminated();
			if(!model.isHumanPlayerEliminated()) {
				view.showPlayerCardOnTop();
				view.showHumanPlayerCardCount();
			}
			view.showRoundNumber();
			//...procedure of confirming current attribute in this round
			for(int i=0;i<model.getPlayers().size();i++) {	//for each player in a round
				if(model.getIndexOfRoundWinner()==i) {								//if current player is the round winner
					if(model.getPlayers().get(i).getPlayerName().equals("You")) {	//if the winner is the user
						view.showCategories();										//show the categories text
						int playerIndex = getAttributeIndex();						//get attribute user chooses from system input
						model.setIndexOfCurrentAttribute(playerIndex);	//let user input the index of characteristic chosen
						view.showPickingMessage(playerIndex);
						model.testLog.addAttributeSelected();

					}else {							//if the winner is an AI player
//						view.showFirstCardOfPlayerByIndex(i);							//show first card
						int AIIndex = getHighestAttributeIndex(i);
						model.setIndexOfCurrentAttribute(AIIndex);	//let the AI choose the characteristic
						view.showPickingMessage(AIIndex);
						model.testLog.addAttributeSelected();
//						model.setIndexOfCurrentCharacteristic(getAttributeIndex());	//let user input the index of characteristic chosen

					}
				}	//characteristic chosen by now...
			}
			view.showCardsThisRound();
			this.playRound(model.getIndexOfCurrentAttribute());
			for(int i=0;i<model.getPlayers().size();i++) {							//for each player
				if(model.getPlayers().get(i).getPlayerCards().size()==40) {		//if anyone have 40 cards
					model.setEnd(true);						//set game end
					model.setIndexOfFinalWinner(i);

					break;
				}
			}
			if(model.isEnd()) {
				break;
			}
			if(model.getCommunalPile().size() + model.getPlayers().get(model.getIndexOfRoundWinner()).getPlayerCards().size() == 40) {
				model.setEnd(true);
				model.setIndexOfFinalWinner(model.getIndexOfRoundWinner());
				
				break;
			}
		}
		view.showFinalWinner();
		model.testLog.addFinalWinner();
		view.showScores();
		setStatistics();
		DbConnection d = new DbConnection();
		System.out.println(d.statisticsToString());
		d.closeConnection(d);
		System.out.println();
	}
	
	//method to play an entire round of this game
	public void playRound(int indexOfCharacteristic) {
		model.initCardsThisRound();
		int max = 0;
		ArrayList<Card> cardsThisRound = new ArrayList<Card>();
 		int temp;		//current card's temp characteristic
		model.setDraw(true);
		int winnerIndex = model.getIndexOfRoundWinner();	//initialise the index of the round winner
		for(int i=0;i<model.getPlayers().size();i++) {		//for each player
			if(model.getPlayers().get(i).getPlayerCards().size()!=0) {					//if the player isn't eliminated
				cardsThisRound.add(model.getPlayers().get(i).getPlayerCards().get(0));	//add current card to the array list fot this round
			}else {
				model.getPlayers().get(i).setEliminated(true);
			}
		}
		for(Card c:cardsThisRound){
			model.getCardsThisRound().add(c);
		}
		model.testLog.addCardsThisRound();
		for(int i=0;i<cardsThisRound.size();i++) {
			temp = cardsThisRound.get(i).getAttributes()[indexOfCharacteristic];	//get current characteristic
			if(temp>=max) {				
				if(temp==max) {		
					model.setDraw(true);
					continue;
				}
				max = temp;
				winnerIndex = cardsThisRound.get(i).getPlayerIndex();	//set the winner index
				model.setDraw(false);
			}
		}
		if(!model.isDraw()) {		//if it is not a draw
			model.setIndexOfRoundWinner(winnerIndex);	//first update the index of round winner in the model
			model.getPlayers().get(winnerIndex).addGameWon();		//add gameWon
			model.setIndexOfWinningCard((model.getPlayers().get(winnerIndex).getPlayerCards().get(0).getCardIndex()));
			for(int i=0;i<cardsThisRound.size();i++) {	//for each cards in this round
				model.getPlayers().get(winnerIndex).getPlayerCards().add(cardsThisRound.get(i));	//add all cards to the winner's cards
				model.getPlayers().get(cardsThisRound.get(i).getPlayerIndex()).getPlayerCards().remove(0);	//remove everyone's first card
				cardsThisRound.get(i).setPlayerIndex(winnerIndex);						//set all cards' winner index
				if(!model.getCommunalPile().isEmpty()) {				//if there is any cards in the communal pile
					model.testLog.addCommunalPile();
					for(int j=0;j<model.getCommunalPile().size();j++) {	//for each card in the communal pile
						model.getPlayers().get(winnerIndex).getPlayerCards().add(model.getCommunalPile().get(j));	//add it to winner's cards
						model.getCommunalPile().get(j).setPlayerIndex(winnerIndex);						//set card's player index to the winner's
					}
					model.getCommunalPile().clear();					//clear all items in the communal pile
				}
			}
			view.showRoundWinner();
			view.showWinningCard();
			model.testLog.addPlayerCardsInfo();
			if(!model.isHumanPlayerEliminated()) {
				if(!isOnline) {					//if it's not online mode
					s.nextLine();               //make the program stop
				}
			}

//			view.showAllPlayerCardsCount();			//for testing
//			s.nextLine();
		}else {				//if it is a draw
			for(int i=0;i<cardsThisRound.size();i++) {						//for each card in this round
				model.getCommunalPile().add(cardsThisRound.get(i));			//add the card to the communal pile
				model.getPlayers().get(cardsThisRound.get(i).getPlayerIndex()).getPlayerCards().remove(0);					//remove everyone's first card
				cardsThisRound.get(i).setPlayerIndex(-1);	//set player index to -1
			}
			model.testLog.addCommunalPile();
			model.addDrawNumbers();
			view.showDrawMessage();
//			view.showAllPlayerCardsCount();			//for testing
			if(!model.isHumanPlayerEliminated()) {
				if(!isOnline) {					//if it's not online mode
					s.nextLine();               //make the program stop
				}
			}
			
		}
		
	}
	
	public void checkHumanPlayerEliminated() {
		if(!model.isHumanPlayerEliminated()) {				//if human player hasn't been marked as eliminated
			if(model.getPlayers().get(model.getIndexOfHumanPlayer()).getPlayerCards().size()==0) {
				model.setHumanPlayerEliminated(true);
				System.out.println("You lose!");
				if(!isOnline) {					//if it's not online mode
					s.nextLine();               //make the program stop
				}
			}
		}
	}

	//method identical for the one used when playing the game, except it doesn't pause the game in order to be suitable JUNit testing
	public void checkHumanPlayerEliminated(String test) {
		if(!model.isHumanPlayerEliminated()) {				//if human player hasn't been marked as eliminated
			if(model.getPlayers().get(model.getIndexOfHumanPlayer()).getPlayerCards().size()==0) {
				model.setHumanPlayerEliminated(true);
				System.out.println("You lose!");
			}
		}
	}
	
	public int getAttributeIndex() {
		int index = 0;
		boolean isValid = false;
		do {
			Scanner s1 = new Scanner(s.nextLine());
			if(s1.hasNextInt()) {
				index = s1.nextInt();
				if(index>0&&index<6) {
					isValid = true;
					break;
				}else {
					System.out.println("Wrong Input!");
				}
			}else {
				System.out.println("Wrong Input!");
			}
			s1.close();
		}while(!isValid);
		
		return index-1;
	}
	

	
	//the AI
	public int getHighestAttributeIndex(int playerIndex) {
		int index = 0;
		int max = 0;
		Card firstCard = model.getPlayers().get(playerIndex).getPlayerCards().get(0);	//get the player's cards on top
		int[] characteristics = firstCard.getAttributes();
		for(int i=0;i<characteristics.length;i++) {	//for each attribute
			if(characteristics[i]>max) {
				max = characteristics[i];
				index = i;
			}
		}
		return index;
	}
	
	public void setStatistics() {
		DbConnection d = new DbConnection();
		int[] r = new int[model.getPlayers().size()];
		for(int i=0;i<model.getPlayers().size();i++) {
			r[i] = model.getPlayers().get(i).getGameWon();
		}
		d.setWinnerPlayer(d,model.getPlayers().get(model.getIndexOfFinalWinner()).hashCode());
		d.setDraws(d, model.getDrawNumbers());
		d.setRounds(d,model.getRoundNumber());
		d.setPlayers(d,model.getPlayers().size());
		d.setPlayerRoundsWon(d,r);
		
		d.insertGameValues(d);
		
		d.closeConnection(d);

	}

	//getters and setters
	public boolean isOnline() {
		return isOnline;
	}

	public void setOnline(boolean online) {
		isOnline = online;
	}

	public boolean isPaused() {
		return isPaused;
	}

	public void setPaused(boolean paused) {
		isPaused = paused;
	}

	public TTModel getModel() {
		return model;}



}
