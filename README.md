# Light-em-all-game

## Description
**Light Em All** is a Java-based puzzle game where players connect tiles to power a grid. The game features a scoring system, interactive UI, and two game modes (traditional and hexagonal). The challenge lies in rotating tiles and positioning the power station strategically to light up the entire grid.

## Features
- **Tile Rotation and Connection**: Rotate tiles to form paths for electricity.
- **Hexagonal and Traditional Modes**: Play with square or hexagonal grids.
- **Scoring System**: Scores are based on time and steps (tile rotations).
- **Dynamic UI**: Interactive user interface with starting, middle, and ending screens.
- **High Score Tracking**: Keeps track of the highest score across games.

## How to Play
1. **Starting the Game**:
   - Run the `LightEmAllApplication` class.
   - Press `Enter` for the traditional grid mode.
   - Press `R` for the hexagonal grid mode.
2. **Game Controls**:
   - Rotate tiles by clicking on them.
   - Move the power station using arrow keys (`W`, `A`, `S`, `D`) or directional keys (`Up`, `Down`, `Left`, `Right`).
   - Press `Esc` to quit the game.
3. **Objective**:
   - Connect all tiles to the power station to light up the grid.

## Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/your-repository.git
   ```
2. Compile the project using a Java IDE or the terminal:
   ```bash
   javac -cp javalib-1.0.jar:. LightEmAllApplication.java
   ```
3. Run the application:
   ```bash
   java -cp javalib-1.0.jar:. LightEmAllApplication
   ```

## Project Structure
```
src/
├── LightEmAllApplication.java  # Main runnable class
├── LightEmAll.java             # Core game logic
├── GamePiece.java              # Representation of individual tiles
├── HexGamePiece.java           # Hexagonal tile logic
├── Edge.java                   # Edge for traditional grids
├── HexEdge.java                # Edge for hexagonal grids
├── UnionFind.java              # Minimum spanning tree helper
├── ExamplesLightEmAll.java     # Test cases
lib/
└── javalib-1.0.jar             # JavaLib library for world and image utilities
```

## Key Classes
- **`LightEmAll`**: The main game world, handling the grid, UI, and game state.
- **`GamePiece`**: Represents individual tiles in the traditional grid.
- **`HexGamePiece`**: Represents individual tiles in the hexagonal grid.
- **`UnionFind`**: Helper class for creating the Minimum Spanning Tree (MST) to connect tiles.
- **`Edge` and `HexEdge`**: Represent connections between tiles.

## Development
### Prerequisites
- Java 8 or higher
- [JavaLib](https://github.com/TeachProgramming/JavaLib)

### Running Tests
Test cases are located in the `ExamplesLightEmAll` class. Run the tests using the `tester.Tester` library:
```bash
java -cp tester.jar:javalib-1.0.jar:. ExamplesLightEmAll
```

## Future Enhancements
- Add difficulty levels (e.g., larger grids or tighter time limits).
- Include animations for tile rotation and power flow.
- Improve UI aesthetics and add sound effects.

## Credits
This game was developed using the [JavaLib](https://github.com/TeachProgramming/JavaLib) library for interactive game development in Java.

