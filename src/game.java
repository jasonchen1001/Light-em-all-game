import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

import javalib.impworld.*;
import javalib.worldimages.*;
import tester.Tester;

/**
 * Features added:
 * 1, Wire color change
 * 2, An UI Interface
 * 3, Scoring System based on time and steps (times player rotate tile, not moving PowerStation)
 */

/**
 * The runnable class for the Game
 */
class LightEmAllApplication {
  public static void main(String[] args) {
    LightEmAll world1 = new LightEmAll(10, 15, 80, 10);
    world1.bigBang(world1.width * world1.tileSize, world1.height * world1.tileSize, .1);
  }
}

/**
 * Represent a Light Then All Game World
 */
class LightEmAll extends World {
  // a list of columns of GamePieces,
  // i.e., represents the board in column-major order
  ArrayList<ArrayList<GamePiece>> board;

  // A board of hexegon
  ArrayList<ArrayList<HexGamePiece>> hexBoard;

  // a list of all nodes
  ArrayList<GamePiece> nodes;

  // a list of all hex nodes
  ArrayList<HexGamePiece> hexNodes;

  // a list of edges of the minimum spanning tree
  ArrayList<Edge> mst;

  // a list of edges of the mst of hex board
  ArrayList<HexEdge> hexMst;

  // the width and height of the board
  public int width;
  public int height;

  // Represent the length of the whole hexgon image
  public int hexLength;

  double UIScale;

  // the current location of the power station,
  // as well as its effective radius
  int powerRow = 0;
  int powerCol = 0;

  // Hexegon version of Power Station position
  int hexPowerRow = 0;
  int hexPowerCol = 0;

  int radius; // The radius of the power

  int randomSeed = 0;

  public int tileSize; // The tile size, also side length of Hexgon
  public int wireWidth; // The wire width

  public boolean launchIndicator = true; // Indicate launching a new game
  public boolean quitGameIndicator = false; // indicate about to quit the game

  int steps = 0; // Steps took in the game
  double time = 0.0; // time used in the game

  ArrayList<Integer> ranking = new ArrayList<Integer>(); // List of Scores

  boolean onUIPage = true; // indicate if on UI page
  boolean onMiddlePage = false; // indicate if on middle page

  WorldScene ws; // The WorldScene

  /**
   * The constructor
   * 
   * @param height    The height of the board (in number of tile, not pixel)
   * @param width     The width of the board (in number of tile, not pixel)
   * @param tileSize  The tileSize
   * @param wireWidth The Width of the Wire
   */
  LightEmAll(int height, int width, int tileSize, int wireWidth) {
    this.width = width;
    this.height = height;
    this.tileSize = tileSize;
    this.wireWidth = wireWidth;

    ranking.add(0);

    if (width * height < 100) {
      UIScale = 0.6;
    }
    else if (width * height >= 100 && width * height < 200) {
      UIScale = 1.0;
    }
    else if (width * height >= 200) {
      UIScale = 1.3;
    }
    else {
      UIScale = 1.0;
    }

    ws = new WorldScene(tileSize * width, tileSize * height);
  }

  public WorldScene makeScene() {
    return ws;
  }

  /**
   * Draw the board
   */
  void drawBoard() {
    for (int row = 0; row < height; row++) {
      for (int col = 0; col < width; col++) {
        GamePiece piece = board.get(row).get(col);
        int x = col * tileSize + tileSize / 2;
        int y = row * tileSize + tileSize / 2;
        // For Test Only:
        // System.out.println("Placing at: " + x + ", " + y);
        WorldImage pieceImage = piece.tileRenderer(tileSize, wireWidth, powerCol, powerRow, radius);
        this.ws.placeImageXY(pieceImage, x, y);
      }
    }
  }

  /**
   * Update each tick
   */
  public void onTick() {
    if (onMiddlePage) {
      drawMiddlePage();
    }
    else if (onUIPage) {
      drawStartingPage();
    }
    else {
      time += .1;
      if (launchIndicator) {
        powerUpdate();
        drawBoard();
        launchIndicator = false;
      }
      if (checkIfAllPowered()) {
        onMiddlePage = true;
        onUIPage = false;
      }
    }
  }

  /**
   * Reset the Game
   */
  void resetGame() {
    refrashPage();
    ranking.add(getScore());
    steps = 0;
    time = 0;
    powerRow = 0;
    powerCol = 0;
    onUIPage = true;
    onMiddlePage = false;
    launchIndicator = true;
  }

  /**
   * detect mouse movement
   */
  public void onMouseReleased(Posn position) {
    if (!onUIPage) {
      rotateGamePiece(position);
      updatePowerStatus();
      steps++;
      drawBoard();
    }
  }

  /**
   * detect key movement
   */
  public void onKeyReleased(String key) {
    if (onMiddlePage) {
      if (key.equals("enter")) {
        resetGame();
      }
      else if (key.equals("escape")) {
        quitGameIndicator = true;
      }
    }
    else {
      if (!onUIPage) {
        if ((key.equals("w") || key.equals("up")) && checkIfCouldMoveThrough(2)) {
          removeOldPowerStation();
          powerRow--;
        }
        else if ((key.equals("s") || key.equals("down")) && checkIfCouldMoveThrough(1)) {
          removeOldPowerStation();
          powerRow++;
        }
        else if ((key.equals("a") || key.equals("left")) && checkIfCouldMoveThrough(4)) {
          removeOldPowerStation();
          powerCol--;
        }
        else if ((key.equals("d") || key.equals("right")) && checkIfCouldMoveThrough(3)) {
          removeOldPowerStation();
          powerCol++;
        }
        else if (key.equals("escape")) {
          quitGameIndicator = true;
        }
        powerUpdate();
        drawBoard();
      }
      else {
        if (key.equals("r")) {
          initGame(true);
        }
        else if (key.equals("enter")) {
          initGame(false);
        }
        else if (key.equals("escape")) {
          quitGameIndicator = true;
        }
      }
    }
    // For test only:
    // System.out.println("Current PowerStation at: " + "Col:" + powerCol + " " +
    // "Row:" + powerRow);
  }

  /**
   * End the World under certain circumstance
   */
  public WorldEnd worldEnds() {
    if (quitGameIndicator) {
      refrashPage();
      drawEndPage("Game Summary: ");
      return new WorldEnd(true, ws);
    }
    return new WorldEnd(false, ws);
  }

  /**
   * To draw the end page with a given string to display on Screen
   * 
   * @param str A String
   */
  void drawEndPage(String str) {
    int intWidth = (int) ((int) tileSize * width);
    int intHeight = (int) ((int) tileSize * height);
    ws.placeImageXY(new RectangleImage(intWidth, intHeight, OutlineMode.SOLID, Color.GRAY),
        width * tileSize / 2, ((int) (height * tileSize * 0.5)));

    ws.placeImageXY(new TextImage("(" + "High Score: " + getHighestScore(),
        tileSize * 0.5 * UIScale, Color.WHITE), width * tileSize / 2,
        ((int) (height * tileSize * 0.55)));

    ws.placeImageXY(new TextImage(str, tileSize * 0.5 * UIScale, Color.WHITE), width * tileSize / 2,
        ((int) (height * tileSize * 0.45)));
  }

  /**
   * To Draw the middle page, the page after each round was passed
   */
  void drawMiddlePage() {
    int intWidth = (int) ((int) tileSize * width);
    int intHeight = (int) ((int) tileSize * height);

    ws.placeImageXY(new RectangleImage(intWidth, intHeight, OutlineMode.SOLID, Color.GRAY),
        width * tileSize / 2, ((int) (height * tileSize * 0.5)));

    ws.placeImageXY(
        new TextImage("Time Used: " + String.format("%.1f", time) + " Seconds",
            tileSize * 0.4 * UIScale, Color.WHITE),
        width * tileSize / 2, ((int) (height * tileSize * 0.3)));

    ws.placeImageXY(new TextImage("Score: " + getScore(), tileSize * 0.4 * UIScale, Color.WHITE),
        width * tileSize / 2, ((int) (height * tileSize * 0.4)));

    ws.placeImageXY(
        new TextImage("High Score: " + getHighestScore(), tileSize * 0.4 * UIScale, Color.WHITE),
        width * tileSize / 2, ((int) (height * tileSize * 0.5)));

    ws.placeImageXY(new TextImage("Press 'Enter' To Go Back to Main Menu", tileSize * 0.4 * UIScale,
        Color.WHITE), width * tileSize / 2, ((int) (height * tileSize * 0.6)));

    ws.placeImageXY(new TextImage("Press 'ESC' To Quit", tileSize * 0.4 * UIScale, Color.WHITE),
        width * tileSize / 2, ((int) (height * tileSize * 0.7)));
  }

  /**
   * Draw the starting page
   */
  void drawStartingPage() {
    int intWidth = (int) ((int) tileSize * width);
    int intHeight = (int) ((int) tileSize * height);

    ws.placeImageXY(new RectangleImage(intWidth, intHeight, OutlineMode.SOLID, Color.GRAY),
        width * tileSize / 2, ((int) (height * tileSize * 0.5)));

    ws.placeImageXY(
        new TextImage("High Score: " + getHighestScore(), tileSize * 0.4 * UIScale, Color.WHITE),
        width * tileSize / 2, ((int) (height * tileSize * 0.70)));

    ws.placeImageXY(new TextImage("Press 'Enter' To Start a Traditional Game",
        tileSize * 0.4 * UIScale, Color.WHITE), width * tileSize / 2,
        ((int) (height * tileSize * 0.50)));

    ws.placeImageXY(
        new TextImage("Press 'R' To Start a Hexagon Game", tileSize * 0.4 * UIScale, Color.WHITE),
        width * tileSize / 2, ((int) (height * tileSize * 0.40)));

    ws.placeImageXY(
        new TextImage("Press 'ESC' To Quit The Game", tileSize * 0.4 * UIScale, Color.WHITE),
        width * tileSize / 2, ((int) (height * tileSize * 0.60)));
  }

  /**
   * Refresh every Image on the World Scene
   */
  void refrashPage() {
    ws = new WorldScene(tileSize * width, tileSize * height);
  }

  /**
   * Check if all GamePieces are powered
   * 
   * @return A boolean
   */
  boolean checkIfAllPowered() {
    for (GamePiece node : nodes) {
      if (!node.powered) {
        return false;
      }
    }
    return true;
  }

  /**
   * Initialize the game
   * 
   * @param isHex A boolean, represent if the game board is in Hexagon
   */
  void initGame(boolean isHex) {
    Random random = new Random();
    this.randomSeed = random.nextInt(1, 10000);
    initBoard();
    this.onUIPage = false;
    randomnizeTileRotation();
  }

  /**
   * Get the highest Score in the ranking list
   * 
   * @return
   */
  int getHighestScore() {
    Collections.sort(ranking);
    return ranking.get(ranking.size() - 1);
  }

  int getScore() {
    int baseScore = width * height + 100;
    int difficultyIndex = ((width * height) / 25) * 5;
    int timeIndex = remainingTimeToScore();
    int stepIndex = remainingStepToScore();

    return baseScore + difficultyIndex - timeIndex - stepIndex;
  }

  int remainingStepToScore() {
    return (int) (5 * Math.log(steps));
  }

  /**
   * The function to calculate how time passed affect one's score
   * 
   * @return The score to be decreased
   */
  int remainingTimeToScore() {
    return (int) (3 * Math.log(time));
  }

  /**
   * Initialize the board, includes: 1, add n nodes to the nodes and the board (n
   * = height * width) 2, receive minimum spanning tree from makeLOE() 3, modify
   * each GamePiece following mst, make sure they are connected correctly 4, call
   * serApproprateRadius() to set radius of the Power Station
   */
  void initBoard() {

    refrashPage();

    this.board = new ArrayList<ArrayList<GamePiece>>();
    this.nodes = new ArrayList<GamePiece>();

    for (int row = 0; row < this.height; row++) {
      ArrayList<GamePiece> column = new ArrayList<GamePiece>();
      for (int col = 0; col < this.width; col++) {
        GamePiece piece = new GamePiece();
        piece.row = row;
        piece.col = col;
        column.add(piece);
        nodes.add(piece);
      }
      this.board.add(column);
    }

    this.mst = makeLOE(randomSeed);

    for (GamePiece node : nodes) {
      for (Edge edge : mst) {
        if (edge.from.equals(node)) {
          if (edge.to.col == node.col - 1) {
            node.left = true;
            edge.to.right = true;
          }
          else if (edge.to.col == node.col + 1) {
            node.right = true;
            edge.to.left = true;
          }
          else if (edge.to.row == node.row + 1) {
            node.bottom = true;
            edge.to.top = true;
          }
          else if (edge.to.row == node.row - 1) {
            node.top = true;
            edge.to.bottom = true;
          }
        }
        else if (edge.to.equals(node)) {
          if (edge.from.col == node.col - 1) {
            node.left = true;
            edge.from.right = true;
          }
          else if (edge.from.col == node.col + 1) {
            node.right = true;
            edge.from.left = true;
          }
          else if (edge.from.row == node.row + 1) {
            node.bottom = true;
            edge.from.top = true;
          }
          else if (edge.from.row == node.row - 1) {
            node.top = true;
            edge.from.bottom = true;
          }
        }
      }
    }

    setApproprateRadiusValue();
  }

  /**
   * Make a minimum spanning tree, includes few steps: 1, make a list of edges,
   * includes all potential edges on the board 2, given each of them a random
   * weight 3, remove repeated edges 4, with UnionFind, make the mst with Kruskal
   * algorithm
   * 
   * @return A list of Edges
   */
  ArrayList<Edge> makeLOE(int randomSeed) {
    ArrayList<Edge> result = new ArrayList<Edge>();
    Random random = new Random(randomSeed);
    for (int row = 0; row < board.size(); row++) {
      int localHeight = board.size();

      for (int col = 0; col < board.get(row).size(); col++) {

        int localWidth = board.get(row).size();
        GamePiece currentNode = board.get(row).get(col);

        if (row == 0 && col == 0) {
          GamePiece rightNode = board.get(row).get(col + 1);
          GamePiece downNode = board.get(row + 1).get(col);
          result.add(new Edge(currentNode, rightNode, random.nextInt(100)));
          result.add(new Edge(currentNode, downNode, random.nextInt(100)));
        }
        else if (col == localWidth - 1 && row == 0) {
          GamePiece leftNode = board.get(row).get(col - 1);
          GamePiece downNode = board.get(row + 1).get(col);
          result.add(new Edge(currentNode, leftNode, random.nextInt(100)));
          result.add(new Edge(currentNode, downNode, random.nextInt(100)));
        }
        else if (row == localHeight - 1 && col == localWidth - 1) {
          GamePiece leftNode = board.get(row).get(col - 1);
          GamePiece upNode = board.get(row - 1).get(col);
          result.add(new Edge(currentNode, leftNode, random.nextInt(100)));
          result.add(new Edge(currentNode, upNode, random.nextInt(100)));
        }
        else if (row == localHeight - 1 && col == 0) {
          GamePiece rightNode = board.get(row).get(col + 1);
          GamePiece upNode = board.get(row - 1).get(col);
          result.add(new Edge(currentNode, rightNode, random.nextInt(100)));
          result.add(new Edge(currentNode, upNode, random.nextInt(100)));
        }
        else if (row == 0) {
          GamePiece rightNode = board.get(row).get(col + 1);
          GamePiece leftNode = board.get(row).get(col - 1);
          GamePiece downNode = board.get(row + 1).get(col);
          result.add(new Edge(currentNode, rightNode, random.nextInt(100)));
          result.add(new Edge(currentNode, downNode, random.nextInt(100)));
          result.add(new Edge(currentNode, leftNode, random.nextInt(100)));
        }
        else if (row == localHeight - 1) {
          GamePiece rightNode = board.get(row).get(col + 1);
          GamePiece leftNode = board.get(row).get(col - 1);
          GamePiece upNode = board.get(row - 1).get(col);
          result.add(new Edge(currentNode, rightNode, random.nextInt(100)));
          result.add(new Edge(currentNode, upNode, random.nextInt(100)));
          result.add(new Edge(currentNode, leftNode, random.nextInt(100)));
        }
        else if (col == 0) {
          GamePiece rightNode = board.get(row).get(col + 1);
          GamePiece downNode = board.get(row + 1).get(col);
          GamePiece upNode = board.get(row - 1).get(col);
          result.add(new Edge(currentNode, rightNode, random.nextInt(100)));
          result.add(new Edge(currentNode, upNode, random.nextInt(100)));
          result.add(new Edge(currentNode, downNode, random.nextInt(100)));
        }
        else if (col == localWidth - 1) {
          GamePiece leftNode = board.get(row).get(col - 1);
          GamePiece downNode = board.get(row + 1).get(col);
          GamePiece upNode = board.get(row - 1).get(col);
          result.add(new Edge(currentNode, leftNode, random.nextInt(100)));
          result.add(new Edge(currentNode, upNode, random.nextInt(100)));
          result.add(new Edge(currentNode, downNode, random.nextInt(100)));
        }
        else {
          GamePiece leftNode = board.get(row).get(col - 1);
          GamePiece rightNode = board.get(row).get(col + 1);
          GamePiece downNode = board.get(row + 1).get(col);
          GamePiece upNode = board.get(row - 1).get(col);
          result.add(new Edge(currentNode, leftNode, random.nextInt(100)));
          result.add(new Edge(currentNode, upNode, random.nextInt(100)));
          result.add(new Edge(currentNode, downNode, random.nextInt(100)));
          result.add(new Edge(currentNode, rightNode, random.nextInt(100)));
        }
      }
    }

    for (int i = 0; i < result.size(); i++) {
      if (result.get(i) != null) {
        for (int j = 0; j < result.size(); j++) {
          if (result.get(j) != null && i != j) {
            if (result.get(i).ifSameEdge(result.get(j))) {
              result.set(i, null);
              break;
            }
          }
        }
      }
    }

    ArrayList<Edge> temp = new ArrayList<Edge>();

    for (int i = 0; i < result.size(); i++) {
      if (result.get(i) != null) {
        temp.add(result.get(i));
      }
    }

    result = temp;

    Collections.sort(result);

    UnionFind uf = new UnionFind(nodes.size());

    ArrayList<Edge> mst = new ArrayList<Edge>();

    for (Edge edge : result) {
      int x = uf.find(nodes.indexOf(edge.from));
      int y = uf.find(nodes.indexOf(edge.to));
      if (x != y) {
        mst.add(edge);
        uf.union(x, y);
      }
      if (mst.size() == nodes.size() - 1) {
        break;
      }
    }

    result = mst;

    // ---------------------------------------------------------------
    // Debugging area BELOW:
    /*
     * for (Edge edge : result) { System.out.println("Node at col:" + edge.from.col
     * + " row:" + edge.from.row + " Linked with Node at col:" + edge.to.col +
     * " row:" + edge.to.row); } System.out.println("Total Number of Edge: " +
     * result.size());
     * 
     * for (Edge edge1 : result) { for (Edge edge2 : result) { if
     * (edge1.from.equals(edge2.to) && edge1.to.equals(edge2.from)) {
     * System.out.println("Found repeated Edge"); } } }
     */
    // Debugging area ABOVE:
    // ---------------------------------------------------------------

    return result;
  }

  /**
   * A combined method of updatePowerStation() and updatePowerStatus()
   */
  void powerUpdate() {
    updatePowerStation();
    updatePowerStatus();
  }

  /**
   * To rotate each tile on the board random times
   */
  void randomnizeTileRotation() {
    Random random = new Random(randomSeed);

    for (GamePiece gp : nodes) {
      for (int i = 0; i < random.nextInt(1, 4); i++) {
        gp.rotate(random.nextBoolean());
      }
    }

  }

  /**
   * through double BFS, find the center of the tree, and set the radius as the
   * maximum length from the center to a leaf node CALL IT AFTER MST WAS
   * INITIALIZED
   */
  void setApproprateRadiusValue() {
    this.radius = findRadius(findTheFarthestNode(nodes.get(0)));
  }

  /**
   * A generic function, available for all object with equals() overwritten, to
   * check if a T is in a List of T
   * 
   * @param <T> A generic type
   * @param lst List of T
   * @param t   A T
   * @return A boolean
   */
  <T> boolean checkIfExistInList(ArrayList<T> lst, T t) {
    for (T element : lst) {
      if (t.equals(element)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Given a GamePiece, find out the farthest node of it in a tree
   * 
   * @param start A GamePiece
   * @return The Farthest GamePiece
   */
  GamePiece findTheFarthestNode(GamePiece start) {

    ArrayList<GamePiece> visited = new ArrayList<GamePiece>();

    Queue<GamePiece> queue = new LinkedList<>();

    queue.add(start);
    visited.add(start);
    GamePiece last = start;

    while (!queue.isEmpty()) {
      last = queue.poll();

      for (GamePiece neighbor : getOriginalNeighbors(last)) {
        if (!checkIfExistInList(visited, neighbor)) {
          visited.add(neighbor);
          queue.add(neighbor);
        }
      }
    }
    return last;
  }

  /**
   * Takes a GamePiece, find the GamePiece in the tree that is farthest from it,
   * the given GamePiece should be result of findTheFarthestNode(GamePiece start)
   * Then, find the longest path from a certain GamePiece to the given GamePiece
   * Then, find the middle point of that path, which should be the center of the
   * tree Then, the length of that path / 2 should be our ideal radius value
   * 
   * @param start A GamePeice
   * @return An int
   */
  int findRadius(GamePiece start) {

    // (Node, ParentNode)
    Map<GamePiece, GamePiece> parent = new HashMap<>();

    // This Part BELOW meant to be the second time BFS, the parent should contain
    // few
    // paths, one of them is the longest path within the tree
    Queue<GamePiece> queue = new LinkedList<>();

    queue.add(start);
    parent.put(start, null);
    GamePiece last = start;

    while (!queue.isEmpty()) {
      last = queue.poll();

      for (GamePiece neighbor : getOriginalNeighbors(last)) {
        if (!parent.containsKey(neighbor)) {
          parent.put(neighbor, last);
          queue.add(neighbor);
        }
      }
    }
    // This Part ABOVE meant to be the second time BFS, the parent should contain
    // few
    // paths, By the end of this part, one of the path is the longest path within
    // the tree

    ArrayList<GamePiece> path = new ArrayList<>();

    for (GamePiece at = last; at != null; at = parent.get(at)) {
      path.add(at);
    }

    return path.size() / 2;

  }

  /**
   * Get a list of a GamePiece's neighbors, only those connected with it by edge
   * 
   * @param gp A GamePiece
   * @return A List of GamePiece
   */
  ArrayList<GamePiece> getOriginalNeighbors(GamePiece gp) {
    ArrayList<GamePiece> result = new ArrayList<GamePiece>();

    for (Edge edge : mst) {
      if (edge.from.equals(gp)) {
        result.add(edge.to);
      }
      else if (edge.to.equals(gp)) {
        result.add(edge.from);
      }
    }

    return result;
  }

  /**
   * Getting the list of current neighbor of a GamePiece, based on checking each
   * tile's up, bottom, left and right, not checking the mst
   * 
   * @param gp A GamePiece
   * @return A List of GemePiece
   */
  ArrayList<GamePiece> getCurrentNeighbors(GamePiece gp) {
    ArrayList<GamePiece> result = new ArrayList<GamePiece>();

    if (gp.col != width - 1
        && gp.checkIfConnected(board.get(gp.row).get(gp.col + 1), height, width)) {
      result.add(board.get(gp.row).get(gp.col + 1));
    }
    if (gp.col != 0 && gp.checkIfConnected(board.get(gp.row).get(gp.col - 1), height, width)) {
      result.add(board.get(gp.row).get(gp.col - 1));
    }
    if (gp.row != 0 && gp.checkIfConnected(board.get(gp.row - 1).get(gp.col), height, width)) {
      result.add(board.get(gp.row - 1).get(gp.col));
    }
    if (gp.row != height - 1
        && gp.checkIfConnected(board.get(gp.row + 1).get(gp.col), height, width)) {
      result.add(board.get(gp.row + 1).get(gp.col));
    }

    return result;
  }

  /**
   * Using BFS, calculate the distance from a certain GamePiece with the
   * PowerStation, if not in range, return -1
   * 
   * @param target The GamePiece need to be checked
   * @return An int
   */
  public int calculateDistance(GamePiece target) {
    if (powerRow == target.row && powerCol == target.col) {
      return 0;
    }

    Queue<GamePiece> queue = new LinkedList<>();
    Map<GamePiece, Integer> distance = new HashMap<>();
    ArrayList<GamePiece> visited = new ArrayList<>();

    GamePiece start = board.get(powerRow).get(powerCol);
    queue.offer(start);
    visited.add(start);
    distance.put(start, 0);

    while (!queue.isEmpty()) {
      GamePiece current = queue.poll();

      ArrayList<GamePiece> neighbors = getCurrentNeighbors(current);
      for (GamePiece neighbor : neighbors) {

        if (!visited.contains(neighbor)) {
          visited.add(neighbor);
          int newDistance = distance.get(current) + 1;
          distance.put(neighbor, newDistance);
          queue.offer(neighbor);

          if (neighbor.row == target.row && neighbor.col == target.col) {
            return newDistance;
          }
        }
      }
    }

    return -1;
  }

  /**
   * Rotate the GamePiece in the given position
   * 
   * @param position Position from Handler
   */
  void rotateGamePiece(Posn position) {
    for (int row = 0; row < board.size(); row++) {
      for (int col = 0; col < board.get(row).size(); col++) {
        int y = (row * tileSize);
        int x = (col * tileSize);
        if ((position.x >= x && position.x <= x + tileSize)
            && (position.y >= y && position.y <= y + tileSize)) {
          board.get(row).get(col).rotate(true);
        }
      }
    }
  }

  /**
   * Takes a direction, which represented as Integer, 1 = down, 2 = up, left = 4,
   * right = 3, Check if the neighbor tile exist, if so, check if the path exists,
   * by checking the left, right, up, bottom of each tile, if all of them exist,
   * return true, otherwise false
   * 
   * @param direction An Integer
   * @return A boolean
   */
  boolean checkIfCouldMoveThrough(int direction) {
    for (int row = 0; row < board.size(); row++) {
      for (int col = 0; col < board.get(row).size(); col++) {
        if (board.get(row).get(col).powerStation == true) {
          GamePiece powerStationTile = board.get(row).get(col);
          if (direction == 1 && powerStationTile.bottom) {
            if (row + 1 < board.size()) {
              return board.get(row + 1).get(col).checkIfConnectedWith(direction);
            }
          }
          else if (direction == 2 && powerStationTile.top) {
            if (row - 1 >= 0) {
              return board.get(row - 1).get(col).checkIfConnectedWith(direction);
            }
          }
          else if (direction == 3 && powerStationTile.right) {
            if (col + 1 < board.get(row).size()) {
              return board.get(row).get(col + 1).checkIfConnectedWith(direction);
            }
          }
          else if (direction == 4 && powerStationTile.left) {
            if (col - 1 >= 0) {
              return board.get(row).get(col - 1).checkIfConnectedWith(direction);
            }
          }
        }
      }
    }
    return false;
  }

  boolean checkIfCouldMoveThroughHex(int direction) {
    for (int layer = 0; layer < hexBoard.size(); layer++) {
      for (int item = 0; item < hexBoard.get(layer).size(); item++) {
        HexGamePiece hex = hexBoard.get(layer).get(item);
        if (hex.powerStation) {
          if (direction == 0) { // N
            return checkConnectionHex(layer - 1, item, hex, direction);
          }
          else if (direction == 1) { // NE
            return checkConnectionHex(layer + 1, item, hex, direction);
          }
          else if (direction == 2) { // SE
            return checkConnectionHex(layer, item + 1, hex, direction);
          }
          else if (direction == 3) { // S
            return checkConnectionHex(layer + 1, item + 1, hex, direction);
          }
          else if (direction == 4) { // SW
            return checkConnectionHex(layer, item - 1, hex, direction);
          }
          else if (direction == 5) { // SN
            return checkConnectionHex(layer - 1, item - 1, hex, direction);
          }
        }
      }
    }
    return false;
  }

  /**
   * Update the status of power station
   */
  void updatePowerStation() {
    for (int row = 0; row < board.size(); row++) {
      for (int col = 0; col < board.get(row).size(); col++) {
        if (col == powerCol && row == powerRow) {
          // System.out.println("PowerStation updated at " + "Col:" + col + " " + "Row:" +
          // row);
          board.get(row).get(col).setPowerStation();
        }
      }
    }
  }

  /**
   * Update the Status of weather powered for GamePiece
   */
  void updatePowerStatus() {
    for (int row = 0; row < board.size(); row++) {
      for (int col = 0; col < board.get(row).size(); col++) {
        if (calculateDistance(board.get(row).get(col)) >= 0
            && calculateDistance(board.get(row).get(col)) <= radius * 0.2) {
          board.get(row).get(col).powerUp(5);
        }
        else if (calculateDistance(board.get(row).get(col)) >= radius * 0.2
            && calculateDistance(board.get(row).get(col)) <= radius * 0.4) {
          board.get(row).get(col).powerUp(4);
        }
        else if (calculateDistance(board.get(row).get(col)) >= radius * 0.4
            && calculateDistance(board.get(row).get(col)) <= radius * 0.6) {
          board.get(row).get(col).powerUp(3);
        }
        else if (calculateDistance(board.get(row).get(col)) >= radius * 0.6
            && calculateDistance(board.get(row).get(col)) <= radius * 0.8) {
          board.get(row).get(col).powerUp(2);
        }
        else if (calculateDistance(board.get(row).get(col)) >= radius * 0.8
            && calculateDistance(board.get(row).get(col)) <= radius) {
          board.get(row).get(col).powerUp(1);
        }
        else {
          board.get(row).get(col).powerDown();
        }
      }
    }
  }

  /**
   * Remove powerStation from the board
   */
  void removeOldPowerStation() {
    for (ArrayList<GamePiece> logp : board) {
      for (GamePiece gp : logp) {
        if (gp.powerStation) {
          gp.powerStation = false;
        }
      }
    }
  }

  void initHexBoard() {
    hexBoard = new ArrayList<>();
    hexNodes = new ArrayList<>();

    for (int layer = 0; layer < this.height; layer++) {
      int itemsInLayer;
      if (layer == 0) {
        itemsInLayer = 1;
      }
      else {
        itemsInLayer = layer * 6;
      }
      ArrayList<HexGamePiece> newLayer = new ArrayList<>();

      for (int item = 0; item < itemsInLayer; item++) {
        HexGamePiece piece = new HexGamePiece();
        piece.layer = layer;
        piece.item = item;
        newLayer.add(piece);
        hexNodes.add(piece);
      }
      this.hexBoard.add(newLayer);
    }

    this.hexMst = makeHexLOE(randomSeed);

    for (HexEdge edge : hexMst) {
      int layerDiff = Math.abs(edge.from.layer - edge.to.layer);
      if (layerDiff == 0) {
        sameLayerConnections(edge);
      }
      else if (layerDiff == 1) {
        differentLayerConnections(edge);
      }
    }
  }

  void sameLayerConnections(HexEdge edge) {
    int indexDiff = edge.to.item - edge.from.item;
    if (indexDiff == 1 || indexDiff == -1) {
      edge.from.southeast = true;
      edge.to.southwest = true;
    }
    else {

    }
  }

  void differentLayerConnections(HexEdge edge) {
    if (edge.from.layer < edge.to.layer) {
      edge.from.northeast = true;
      edge.to.southwest = true;
    }
    else {
      edge.from.southwest = true;
      edge.to.northeast = true;
    }
  }

  ArrayList<HexEdge> makeHexLOE(int randomSeed) {
    ArrayList<HexEdge> result = new ArrayList<HexEdge>();
    Random random = new Random(randomSeed);
    for (int layer = 0; layer < hexBoard.size(); layer++) {
      int numItemsInLayer = hexBoard.get(layer).size();

      for (int item = 0; item < numItemsInLayer; item++) {
        HexGamePiece currentHexNode = hexBoard.get(layer).get(item);

        int prev = item - 1;
        if (prev < 0) {
          prev += numItemsInLayer;
        }
        int next = (item + 1) % numItemsInLayer;

        result.add(addHexEdge(currentHexNode, hexBoard.get(layer).get(prev), random.nextInt(100)));
        result.add(addHexEdge(currentHexNode, hexBoard.get(layer).get(next), random.nextInt(100)));

        if (layer < hexBoard.size() - 1) {
          int outerLayerSize = hexBoard.get(layer + 1).size();
          int firstOuterNeighbor = (item * 2) % outerLayerSize;
          int secondOuterNeighbor = (firstOuterNeighbor + 1) % outerLayerSize;

          result.add(addHexEdge(currentHexNode, hexBoard.get(layer + 1).get(firstOuterNeighbor),
              random.nextInt(100)));
          result.add(addHexEdge(currentHexNode, hexBoard.get(layer + 1).get(secondOuterNeighbor),
              random.nextInt(100)));
        }

        if (layer > 0) {
          int innerLayerSize = hexBoard.get(layer - 1).size();
          int innerNeighbor = item / 2;
          if (innerNeighbor < innerLayerSize) {
            result.add(addHexEdge(currentHexNode, hexBoard.get(layer - 1).get(innerNeighbor),
                random.nextInt(100)));
          }
        }
      }
    }

    for (int i = 0; i < result.size(); i++) {
      if (result.get(i) != null) {
        for (int j = 0; j < result.size(); j++) {
          if (result.get(j) != null && i != j) {
            if (result.get(i).ifSameHexEdge(result.get(j))) {
              result.set(i, null);
              break;
            }
          }
        }
      }
    }

    ArrayList<HexEdge> temp = new ArrayList<HexEdge>();

    for (int i = 0; i < result.size(); i++) {
      if (result.get(i) != null) {
        temp.add(result.get(i));
      }
    }

    result = temp;

    Collections.sort(result);

    UnionFind uf = new UnionFind(nodes.size());

    ArrayList<Edge> mst = new ArrayList<Edge>();

    for (HexEdge hexEdge : result) {
      int x = uf.find(nodes.indexOf(hexEdge.from));
      int y = uf.find(nodes.indexOf(hexEdge.to));
      if (x != y) {
        hexMst.add(hexEdge);
        uf.union(x, y);
      }
      if (mst.size() == nodes.size() - 1) {
        break;
      }
    }

    result = hexMst;

    return result;
  }

  HexEdge addHexEdge(HexGamePiece from, HexGamePiece to, int weight) {
    return new HexEdge(from, to, weight);
  }

  /**
   * Represent a Game Piece
   */
  class GamePiece {
    // in logical coordinates, with the origin
    // at the top-left corner of the screen
    int row;
    int col;
    // whether this GamePiece is connected to the
    // adjacent left, right, top, or bottom pieces
    boolean left;
    boolean right;
    boolean top;
    boolean bottom;
    // whether the power station is on this piece
    boolean powerStation;
    boolean powered;

    int powerLevel;

    /**
     * To render a tile
     * 
     * @param size      The size of the tile
     * @param wireWidth The width of the wire
     * @param powerCol  The col of PowerStation
     * @param powerRow  The row of PowerStation
     * @param radius    The radius
     * @return A image of the tile
     */
    WorldImage tileRenderer(int size, int wireWidth, int powerCol, int powerRow, int radius) {
      Color brightYellow = new Color(255, 255, 0);
      Color lighterYellow = new Color(255, 230, 0);
      Color mediumYellow = new Color(255, 204, 0);
      Color darkerYellow = new Color(255, 179, 0);
      Color darkYellow = new Color(255, 153, 0);
      if (powerLevel == 1) {
        return tileImage(size, wireWidth, darkYellow, this.powerStation);
      }
      else if (powerLevel == 2) {
        return tileImage(size, wireWidth, darkerYellow, this.powerStation);
      }
      else if (powerLevel == 3) {
        return tileImage(size, wireWidth, mediumYellow, this.powerStation);
      }
      else if (powerLevel == 4) {
        return tileImage(size, wireWidth, lighterYellow, this.powerStation);
      }
      else if (powerLevel == 5) {
        return tileImage(size, wireWidth, brightYellow, this.powerStation);
      }
      else {
        return tileImage(size, wireWidth, Color.GRAY, this.powerStation);
      }
    }

    /**
     * To render a tile
     * 
     * @param size            Size of tile
     * @param wireWidth       Width of wire
     * @param wireColor       Color of the wire
     * @param hasPowerStation If has Power Station
     * @return A WorldImage
     */
    WorldImage tileImage(int size, int wireWidth, Color wireColor, boolean hasPowerStation) {
      // Start tile image off as a blue square with a wire-width square in the middle,
      // to make image "cleaner" (will look strange if tile has no wire, but that
      // can't be)
      WorldImage image = new OverlayImage(
          new RectangleImage(wireWidth, wireWidth, OutlineMode.SOLID, wireColor),
          new RectangleImage(size, size, OutlineMode.SOLID, Color.DARK_GRAY));
      WorldImage vWire = new RectangleImage(wireWidth, (size + 1) / 2, OutlineMode.SOLID,
          wireColor);
      WorldImage hWire = new RectangleImage((size + 1) / 2, wireWidth, OutlineMode.SOLID,
          wireColor);

      if (this.top) {
        image = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP, vWire, 0, 0, image);
      }
      if (this.right) {
        image = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE, hWire, 0, 0, image);
      }
      if (this.bottom) {
        image = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM, vWire, 0, 0, image);
      }
      if (this.left) {
        image = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE, hWire, 0, 0, image);
      }
      if (hasPowerStation) {
        image = new OverlayImage(new OverlayImage(
            new StarImage(size / 3, 7, OutlineMode.OUTLINE, new Color(255, 128, 0)),
            new StarImage(size / 3, 7, OutlineMode.SOLID, new Color(0, 255, 255))), image);
      }
      // System.out.println("Image Placed");
      return image;
    }

    /**
     * To set the power sattion on specific tile
     */
    void setPowerStation() {
      this.powerStation = true;
    }

    /**
     * Check if a tile is within the radius of power
     * 
     * @param powerCol powerCol
     * @param powerRow powerRow
     * @param radius   Radius of power
     * @return a float
     */
    double checkIfWithinPowerRadius(int powerCol, int powerRow, int radius) {
      double distance = Math.abs(this.col - powerCol) + Math.abs(this.row - powerRow);
      return ((double) radius) - distance;
    }

    boolean checkIfConnectedWith(int direction) {
      if (direction == 1 && top) {
        return true;
      }
      else if (direction == 2 && bottom) {
        return true;
      }
      else if (direction == 3 && left) {
        return true;
      }
      else if (direction == 4 && right) {
        return true;
      }
      return false;
    }

    /**
     * To set the powered as true
     */
    void powerUp(int powerLevel) {
      this.powered = true;
      this.powerLevel = powerLevel;
    }

    void powerDown() {
      this.powered = false;
      this.powerLevel = 0;
    }

    /**
     * to Rotate a GamePiece, false = left, true = right
     * 
     * @param direction A boolean
     */
    void rotate(boolean direction) {
      if (direction) {
        boolean temp = this.top;
        this.top = this.left;
        this.left = this.bottom;
        this.bottom = this.right;
        this.right = temp;
      }
      else {
        boolean temp = this.top;
        this.top = this.right;
        this.right = this.bottom;
        this.bottom = this.left;
        this.left = temp;
      }
    }

    public boolean equals(GamePiece other) {
      return this.top == other.top && this.left == other.left && this.bottom == other.bottom
          && this.right == other.right && this.col == other.col && this.row == other.row;
    }

    /**
     * To check if a tile is connnected with a given tile
     * 
     * @param other A GamePiece
     * @return A boolean
     */
    boolean checkIfConnected(GamePiece other, int height, int width) {
      // Check right connection
      if (this.col != width - 1 && this.col + 1 == other.col && this.row == other.row) {
        return this.right && other.left;
      }
      // Check left connection
      if (this.col != 0 && this.col - 1 == other.col && this.row == other.row) {
        return this.left && other.right;
      }
      // Check bottom connection
      if (this.row != height - 1 && this.row + 1 == other.row && this.col == other.col) {
        return this.bottom && other.top;
      }
      // Check top connection
      if (this.row != 0 && this.row - 1 == other.row && this.col == other.col) {
        return this.top && other.bottom;
      }
      return false; // No connection if none of the conditions are met
    }
  }

  class HexGamePiece {
    int layer;
    int item;
    boolean north, northeast, southeast, south, southwest, northwest;
    boolean powerStation;
    boolean powered;
    int powerLevel;

    void rotateHexGamePiece() {
      boolean temp = north;
      north = northwest;
      northwest = southwest;
      southwest = south;
      south = southeast;
      southeast = northeast;
      northeast = temp;
    }

    WorldImage tileRendererHex(int size, int wireWidth) {
      Color brightYellow = new Color(255, 255, 0);
      Color lighterYellow = new Color(255, 230, 0);
      Color mediumYellow = new Color(255, 204, 0);
      Color darkerYellow = new Color(255, 179, 0);
      Color darkYellow = new Color(255, 153, 0);
      if (powerLevel == 1) {
        return tileImageHex(size, wireWidth, darkYellow, this.powerStation);
      }
      else if (powerLevel == 2) {
        return tileImageHex(size, wireWidth, darkerYellow, this.powerStation);
      }
      else if (powerLevel == 3) {
        return tileImageHex(size, wireWidth, mediumYellow, this.powerStation);
      }
      else if (powerLevel == 4) {
        return tileImageHex(size, wireWidth, lighterYellow, this.powerStation);
      }
      else if (powerLevel == 5) {
        return tileImageHex(size, wireWidth, brightYellow, this.powerStation);
      }
      else {
        return tileImageHex(size, wireWidth, Color.GRAY, this.powerStation);
      }
    }

    WorldImage tileImageHex(int size, int wireWidth, Color wireColor, boolean hasPowerStation) {

      WorldImage hex = new HexagonImage(size, OutlineMode.SOLID, wireColor);

      WorldImage vWire = new RectangleImage(wireWidth, size / 2, OutlineMode.SOLID, wireColor);

      if (this.north) {
        hex = new RotateImage(
            new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP, vWire, 0, -size / 4, hex), 0);
      }
      if (this.northeast) {
        hex = new RotateImage(
            new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP, vWire, 0, -size / 4, hex),
            60);
      }
      if (this.southeast) {
        hex = new RotateImage(
            new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP, vWire, 0, -size / 4, hex),
            120);
      }
      if (this.south) {
        hex = new RotateImage(
            new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP, vWire, 0, -size / 4, hex),
            180);
      }
      if (this.southwest) {
        hex = new RotateImage(
            new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP, vWire, 0, -size / 4, hex),
            240);
      }
      if (this.northwest) {
        hex = new RotateImage(
            new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP, vWire, 0, -size / 4, hex),
            300);
      }
      if (hasPowerStation) {
        hex = new OverlayImage(new OverlayImage(
            new StarImage(size / 3, 7, OutlineMode.OUTLINE, new Color(255, 128, 0)),
            new StarImage(size / 3, 7, OutlineMode.SOLID, new Color(0, 255, 255))), hex);
      }

      return hex;
    }

    /**
     * Check if the hex and the HexGamePeice at given layer and item are connected
     * at the given direction
     * 
     * @param layer     The layer of a HexGamePeice
     * @param item      The item of a HexGamePiece
     * @param hex       A HexGamePiece
     * @param direction An int, from 0 to 5
     * @return A boolean
     */
    boolean checkConnectionHex(int layer, int item, HexGamePiece hex, int direction) {
      if (layer >= 0 && layer < hexBoard.size() && item >= 0 && item < hexBoard.get(layer).size()) {
        HexGamePiece neighbor = hexBoard.get(layer).get(item);
        return hex.checkIfConnectedWithHex(neighbor, direction);
      }
      return false;
    }

    /**
     * Check if the invoking HexGamePiece and the given HexGamePiece are connected
     * 
     * @param hex       An HexGamePiece
     * @param direction A int, from 0 to 5
     * @return A boolean
     */
    boolean checkIfConnectedWithHex(HexGamePiece hex, int direction) {
      if (direction == 0) { // N
        return this.north && hex.south;
      }
      else if (direction == 1) { // NE
        return this.northeast && hex.southwest;
      }
      else if (direction == 2) { // SE
        return this.southeast && hex.northwest;
      }
      else if (direction == 3) { // S
        return this.south && hex.north;
      }
      else if (direction == 4) { // SW
        return this.southwest && hex.northeast;
      }
      else if (direction == 5) { // SE
        return this.southeast && hex.northwest;
      }
    }

  }

  /**
   * Representing an Edge in the tree
   */
  class Edge implements Comparable<Edge> {
    GamePiece from; // The GamePiece of beginning
    GamePiece to; // The GamePiece of ending
    int weight; // The weight of the edge

    /**
     * The constructor
     * 
     * @param from   from
     * @param to     to
     * @param weight weight
     */
    Edge(GamePiece from, GamePiece to, int weight) {
      this.from = from;
      this.to = to;
      this.weight = weight;
    }

    /**
     * Check if two edges are the same, based on if they just swapped from and to
     * 
     * @param other An Edge
     * @return A boolean
     */
    boolean ifSameEdge(Edge other) {
      return this.from.equals(other.to) && this.to.equals(other.from);
    }

    /**
     * Compare two edge with their weight
     */
    public int compareTo(Edge other) {
      return this.weight - other.weight;
    }
  }

  class HexEdge implements Comparable<HexEdge> {
    HexGamePiece from;
    HexGamePiece to;
    int weight;

    HexEdge(HexGamePiece from, HexGamePiece to, int weight) {
      this.from = from;
      this.to = to;
      this.weight = weight;
    }

    boolean ifSameHexEdge(HexEdge other) {
      return this.from.equals(other.to) && this.to.equals(other.from);
    }

    public int compareTo(HexEdge other) {
      return this.weight - other.weight;
    }
  }

  /**
   * Represent a UnionFind structure, used in making minimum spanning tree
   */
  class UnionFind {
    int[] parent; // The array that contains index -> parent of node on that index on nodes
    int[] rank; // The array that conatains ranks of the node on the index

    /**
     * The constructor
     * 
     * @param n An int
     */
    UnionFind(int n) {
      parent = new int[n];
      rank = new int[n];

      for (int i = 0; i < n; i++) {
        parent[i] = i;
        rank[i] = 0;
      }
    }

    /**
     * Determine if two nodes have one parent in the tree
     * 
     * @param i An index
     * @return A boolean
     */
    int find(int i) {
      if (parent[i] != i) {
        parent[i] = find(parent[i]);
      }
      return parent[i];
    }

    /**
     * Combine two nodes, make their parent the same
     * 
     * @param x Index of a node
     * @param y Index of another node
     */
    void union(int x, int y) {
      int rootX = find(x);
      int rootY = find(y);
      if (rootX != rootY) {
        if (rank[rootX] > rank[rootY]) {
          parent[rootY] = rootX;
        }
        else if (rank[rootX] < rank[rootY]) {
          parent[rootX] = rootY;
        }
        else {
          parent[rootY] = rootX;
          rank[rootX]++;
        }
      }
    }

  }

  class ExamplesLightEmAll {
    GamePiece piece;
    LightEmAll game;

    void init() {

      int width = 5;
      int height = 5;
      int radius = 3;
      int tileSize = 80;
      int wireWidth = 10;

      game = new LightEmAll(width, height, tileSize, wireWidth);

      piece = game.board.get(2).get(2);

      piece.left = false;
      piece.right = false;
      piece.top = false;
      piece.bottom = false;
    }

    void testRotate(Tester t) {
      init();

      piece.left = true;
      piece.right = false;
      piece.top = false;
      piece.bottom = false;
      piece.rotate(true);

      t.checkExpect(piece.top, true);
      t.checkExpect(piece.right, false);
      t.checkExpect(piece.bottom, false);
      t.checkExpect(piece.left, false);

      piece.rotate(true);
      t.checkExpect(piece.top, false);
      t.checkExpect(piece.right, true);
      t.checkExpect(piece.bottom, false);
      t.checkExpect(piece.left, false);
    }

    void testPowerUp(Tester t) {
      init();
      t.checkExpect(piece.powered, false);
      piece.powerUp(1);
      t.checkExpect(piece.powered, true);
    }

    void testCheckIfWithinPowerRadius(Tester t) {
      init();
      t.checkExpect(game.board.get(2).get(2).checkIfWithinPowerRadius(2, 2, 1), true);
      t.checkExpect(game.board.get(1).get(2).checkIfWithinPowerRadius(2, 2, 1), true);
      t.checkExpect(game.board.get(3).get(2).checkIfWithinPowerRadius(2, 2, 1), true);
      t.checkExpect(game.board.get(2).get(1).checkIfWithinPowerRadius(2, 2, 1), true);
      t.checkExpect(game.board.get(2).get(3).checkIfWithinPowerRadius(2, 2, 1), true);
      t.checkExpect(game.board.get(0).get(2).checkIfWithinPowerRadius(2, 2, 1), false);
    }

  }
}
