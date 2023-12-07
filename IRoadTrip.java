import java.util.*;
import java.io.*;

public class IRoadTrip {

    private Map<String, List<String>> bordersMap;
    private Map<String, Map<String, Integer>> distanceMap;
    private Map<String, String> countryNameMap;

    public IRoadTrip(String[] args) {
        bordersMap = new HashMap<>();
        distanceMap = new HashMap<>();
        countryNameMap = new HashMap<>();

        if (args.length != 3) {
            System.err.println("Usage: java IRoadTrip borders.txt capdist.csv state_name.tsv");
            System.exit(1);
        }

        readBordersFile(args[0]);
        readDistanceFile(args[1]);
        readStateNameFile(args[2]);
    }

    //Read the borders file and populate the bordersMap
    private void readBordersFile(String bordersFilePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(bordersFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=");
                String country = parts[0].trim();
                String[] borders = parts[1].split(";");


                bordersMap.put(country, Arrays.asList(borders));
            }
        }
        catch (IOException e) {
            System.err.println("Error reading borders file: " + e.getMessage());
            System.exit(1);
        }
    }

    //Read the distance file and populate the distanceMap
    private void readDistanceFile(String distanceFilePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(distanceFilePath))) {
            String line;
            reader.readLine();

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 5) {
                    String code1 = parts[1].trim();
                    String code2 = parts[3].trim();

                    try {
                        int distance = Integer.parseInt(parts[4].trim());

                        distanceMap.computeIfAbsent(code1, k -> new HashMap<>()).put(code2, distance);
                        distanceMap.computeIfAbsent(code2, k -> new HashMap<>()).put(code1, distance);
                    }
                    catch (NumberFormatException e) {
                        System.err.println("Error parsing distance: " + e.getMessage());
                        System.exit(1);
                    }
                }
                else {
                    System.err.println("Invalid format in distance file: " + line);
                    System.exit(1);
                }
            }
        }
        catch (IOException e) {
            System.err.println("Error reading distance file: " + e.getMessage());
            System.exit(1);
        }
    }

    //Read the state name file and populate the countryNameMap
    private void readStateNameFile(String stateNameFilePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(stateNameFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                String code = parts[1].trim();
                String name = parts[2].trim();
                countryNameMap.put(code, name);
            }
        }
        catch (IOException e) {
            System.err.println("Error reading state name file: " + e.getMessage());
            System.exit(1);
        }
    }

    //Get the distance between two countries
    public int getDistance(String country1, String country2) {
        //Check if the countries are present in the distanceMap
        if (!distanceMap.containsKey(country1) || !distanceMap.containsKey(country2)) {
            return -1; // One or both countries do not exist
        }

        //Check if there is a direct distance between the countries
        if (!distanceMap.get(country1).containsKey(country2)) {
            return -1; // No direct distance between countries
        }

        //Retrieve the distance from distanceMap
        return distanceMap.get(country1).get(country2);
    }


    //Find the path between two countries using Dijkstra's algorithm
    public List<String> findPath(String country1, String country2) {
        String code1 = getKeyByValue(countryNameMap, country1);
        String code2 = getKeyByValue(countryNameMap, country2);
        String countryName = "";
        int tentativeDistance = 0;

        if (code1 == null || code2 == null || !bordersMap.containsKey(country1) || !bordersMap.containsKey(country2)) {
            System.out.println("Invalid country names: " + country1 + ", " + country2);
            return Collections.emptyList();
        }


        Map<String, Integer> distances = new HashMap<>();
        Set<String> visited = new HashSet<>();


        PriorityQueue<String> queue = new PriorityQueue<>(Comparator.comparingInt(distances::get));


        distances.put(code1, 0);
        queue.add(code1);


        while (!queue.isEmpty()) {

            String current = queue.poll();


            if (current.equals(code2)) {
                return Collections.singletonList(country1 + " --> " + country2 + " (" + distances.get(current) + " km)");
            }


            visited.add(current);


            for (String neighbor : bordersMap.get(country1)) {
                if (!visited.contains(neighbor)) {
                    countryName = extractCountryName(neighbor);
                    tentativeDistance = getDistance(code1, getKeyByValue(countryNameMap, countryName));
                    if (tentativeDistance < distances.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                        distances.put(neighbor, tentativeDistance);
                        queue.add(neighbor);
                    }
                }
                System.out.println(country1 + " --> " + countryName + " (" + tentativeDistance + " km.)");
                country1 = countryName;
            }


        }


        System.out.println("No path found between " + country1 + " and " + country2);
        return Collections.emptyList();


    }

    //Get the key (country code) by a given value (country name) in a map
    private String getKeyByValue(Map<String, String> map, String value) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static String extractCountryName(String input) {
        //Split the string based on the presence of digits
        String[] parts = input.split("\\d+");

        //Use the first part as the country name
        String countryName = parts[0].trim();

        return countryName;
    }


    //Accept user input to find paths between countries
    public void acceptUserInput() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("Enter the name of the first country (type EXIT to quit): ");
            String country1 = scanner.nextLine();

            if (country1.equalsIgnoreCase("EXIT")) {
                break;
            }

            if (!countryNameMap.containsValue(country1) && !bordersMap.containsKey(country1)) {
                System.out.println("Invalid country name. Please enter a valid country name.");
                continue;
            }

            System.out.print("Enter the name of the second country (type EXIT to quit): ");
            String country2 = scanner.nextLine();

            if (country2.equalsIgnoreCase("EXIT")) {
                break;
            }

            if (!countryNameMap.containsValue(country2)) {
                System.out.println("Invalid country name. Please enter a valid country name.");
                continue;
            }

            System.out.println("Calling findPath...");
            List<String> path = findPath(country1, country2);

            if (path.isEmpty()) {
                System.out.println("No path found between " + country1 + " and " + country2);
            }
            else {
                System.out.println("Route from " + country1 + " to " + country2 + ":");
                for (String step : path) {
                    System.out.println("* " + step);
                }
            }
        }

        scanner.close();
    }

    public static void main(String[] args) {
        IRoadTrip roadTrip = new IRoadTrip(args);
        roadTrip.acceptUserInput();
    }
}
