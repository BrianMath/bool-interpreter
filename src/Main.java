import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
	private static Scanner sc;
	
	public static void setScanner(String file) throws IOException {
		sc = new Scanner(new File(file));
	}
	
	public static void main(String[] args) throws IOException {
		run("src/main.bool");
	}
	
	// Regexes
	final static String[] regexes = {
			"(\\s*)return\\s+([a-zA-Z]+)",															// return
			"(\\s*)([a-zA-Z]+)\\.?([a-zA-Z]+)?\\s+=\\s+([a-zA-Z|0-9]+)\\.?([a-zA-Z]+)?",							// attribution
			"\\s*if\\s+([a-zA-Z]+)\\s+([a-zA-Z]+)\\s+([a-zA-Z]+)\\s+then",							// if
			"(\\s*)([a-zA-Z]+)\\s+=\\s+([a-zA-Z]+)\\s+([+*/-])\\s+([a-zA-Z]+)",
			"(\\s*)([a-zA-Z]+)\\s+=\\s+new\\s+([a-zA-Z]+)",
			"(\\s*)([a-zA-Z]+)\\s+=\\s+([a-zA-Z]+)\\.([a-zA-Z]+)\\(([^)]*)\\)",
			"(\\s*)()([a-zA-Z]+)\\.([a-zA-Z]+)\\(([^)]*)\\)"
	};
	
	public static boolean isNumber(String term) throws IOException {
		Pattern patternType = Pattern.compile("[0-9]+");
		Matcher matcher = patternType.matcher(term);
		
		return matcher.matches();
	}
	
	public static boolean isName(String term) throws IOException {
		Pattern patternType = Pattern.compile("[a-zA-Z]+");
		Matcher matcher = patternType.matcher(term);
		
		return matcher.matches();
	}
	
	public static List<String> compile(String line) throws IOException {
		List<String> str = new ArrayList<>();
		
		boolean matched = false;
		
		// Comparar com todos os regexes
		for (int i = 0; i < regexes.length; i++) {
			Pattern pattern = Pattern.compile(regexes[i]);
			Matcher matcher = pattern.matcher(line);
			
			if (!matcher.matches()) { continue; }
			
			// return
			if (i == 0) {
				str.add(matcher.group(1) + "load " + matcher.group(2));
				str.add(matcher.group(1) + "ret");
				matched = true;
				break;
			}
			
			// attribution
			if (i == 1) {
				String lhs = matcher.group(2);
				String rhs = matcher.group(4);
				
				if (isNumber(rhs)) {
					str.add(matcher.group(1) + "const " + rhs);
				} else if (isName(rhs)) {
					str.add(matcher.group(1) + "load " + rhs);
					
					if (matcher.group(5) != null) {
						str.add(matcher.group(1) + "get " + matcher.group(5));
					}
				}
				
				if (matcher.group(3) != null) {
					str.add(matcher.group(1) + "load " + lhs);
					str.add(matcher.group(1) + "set " + matcher.group(3));
				} else {
					str.add(matcher.group(1) + "store " + lhs);
				}
				
				matched = true;
				break;
			}
			
			// if
			if (i == 2) {
				str.add("load " + matcher.group(1));
				str.add("load " + matcher.group(3));
				str.add(matcher.group(2));
				str.add("if 0");
				
				String nextLine = sc.nextLine();
				int countIf = 0, countElse = 0;
				boolean temElse = false;
				
				while (!nextLine.matches("\\s*end-if") && sc.hasNextLine()) {
					// voltar na linha do if e modificar ela com countIf
					List<String> str2 = compile(nextLine);
					
					// se str2 tiver linha em branco não pode aumentar os contadores
					if (!str2.getFirst().trim().isEmpty()) {
						countIf += str2.size();
						
						if (temElse) {
							countElse++;
						}
					}
					
					str.addAll(str2);
					nextLine = sc.nextLine();
					
					if (nextLine.matches("\\s*else")) {
						temElse = true;
						str.add("else 0");
					}
				}
				
				if (temElse) {
					countIf -= countElse;
					
					int indexElse = str.indexOf("else 0");
					str.set(indexElse, "else " + countElse);
				}
				
				int indexIf = str.indexOf("if 0");
				str.set(indexIf, "if " + countIf);
				
				matched = true;
				break;
			}
			
			// Operações aritméticas
			if (i == 3) {
				str.add(matcher.group(1) + "load " + matcher.group(3));
				str.add(matcher.group(1) + "load " + matcher.group(5));
				switch (matcher.group(4)) {
					case "+":
						str.add(matcher.group(1) + "add");
						break;
					case "-":
						str.add(matcher.group(1) + "sub");
						break;
					case "*":
						str.add(matcher.group(1) + "mul");
						break;
					case "/":
						str.add(matcher.group(1) + "div");
						break;
					default:
						break;
				}
				
				str.add(matcher.group(1) + "store " + matcher.group(2));		
				
				matched = true;
				break;
			}
			
			// Object creation
			if (i == 4) {
				str.add(matcher.group(1) + "new " + matcher.group(3));
				str.add(matcher.group(1) + "store " + matcher.group(2));
				
				matched = true;
				break;
			}
			
			//    1        2                     3            4          5
			// "(\\s*)([a-zA-Z]+)?\\s+=?\\s+([a-zA-Z]+)\\.([a-zA-Z]+)\\((.)\\)"
			// Functions
			if (i == 5 || i == 6) {
				String aux = matcher.group(5);
				if (!aux.isEmpty()) {
					String[] variArray = aux.split(", ");
					
					for (String vari : variArray) {
						str.add(matcher.group(1) + "load " + vari);
					}
				}	
				
				str.add(matcher.group(1) + "load " + matcher.group(3));
				str.add(matcher.group(1) + "call " + matcher.group(4));
				
				if (i == 5) {
					str.add(matcher.group(1) + "store " + matcher.group(2));
				}
					
				matched = true;
				break;
			}
		}
		
		if (!matched) {
			str.add(line);
		}
		
		return str;
	}
	
	public static void run(String name) throws IOException {
		String newName = createFile(name);
		
		setScanner(name);
		FileWriter fw = new FileWriter(newName);
		
		List<String> outfile = new ArrayList<>();
		
		while (sc.hasNextLine()) {
			String line = sc.nextLine();
			List<String> str = compile(line);
			outfile.addAll(str);
		}
		
		Files.write(Paths.get(newName), outfile);
		
		fw.close();
		sc.close();
	}
	
	public static String createFile(String name) {
		String newName = name + "c";
		
		try {
			
			var newFile = new File(newName);
			
			if (newFile.createNewFile()) {
				System.out.println("arquivo criado: " + newFile.getName());
			} else {
				System.out.println("arquivo ja existe");
			}
			
		} catch (IOException e) {
			System.out.println("erro");
			e.printStackTrace();
		}
		
		return newName;
	}
}
