package com.example.demo.limitsservice;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.limitsservice.bean.Employee;
import com.example.demo.limitsservice.bean.LimitConfiguration;
import com.example.demo.limitsservice.constant.Constants;

@RestController
public class LimitsConfigurationController {
	@GetMapping("/limits")
	public LimitConfiguration retriveLimitsFromConfigurations() {
		return new LimitConfiguration(1000, 1);
	}

	@GetMapping("/employees")
	public List<Employee> getEmployees() {
		return getEmpsFromFile();
	}

	@PostMapping("/employee")
	public String createEmployee(@RequestParam int employeeId, @RequestParam String employeeName) {
		createEmpRecordInFile(employeeId, employeeName);
		return "Success";
	}

	@PutMapping("/employee")
	public String updateEmployee(@RequestParam int employeeId, @RequestParam String employeeName) {
		updateEmpRecordInFile(employeeId, employeeName);
		return "Success";
	}

	@DeleteMapping("/employee")
	public String deleteEmployee(@RequestParam int employeeId, @RequestParam String employeeName) {
		deleteEmpRecordInFile(employeeId, employeeName);
		removeBlankLine();
		return "Success";
	}

	void createEmpRecordInFile(int id, String name) {
		Path path = Paths.get(
				Constants.filePath);
		String nLine = System.lineSeparator() + id + "," + name;

		try {
			Files.write(path, nLine.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE,
					StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void updateEmpRecordInFile(int id, String name) {
		String filePath = Constants.filePath;
		Scanner sc = null;
		FileWriter writer = null;
		try {
			sc = new Scanner(new File(filePath));
			StringBuffer buffer = new StringBuffer();
			String oldLine = "";
			while (sc.hasNextLine()) {
				String curLine = sc.nextLine();
				if (curLine.contains(String.valueOf(id))) {
					oldLine = curLine;
				}
				buffer.append(curLine + System.lineSeparator());
			}
			String fileContents = buffer.toString().trim();
			sc.close();
			String newLine = id + "," + name;
			fileContents = fileContents.replace(oldLine, newLine);
			writer = new FileWriter(filePath);
			writer.append(fileContents);
			writer.flush();
			writer.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			sc.close();
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	void removeBlankLine() {

		try {
			String fileName = Constants.filePath;
			long noOfLines = -1;

			try (Stream<String> fileStream = Files.lines(Paths.get(fileName))) {
				// Lines count
				noOfLines = (int) fileStream.count();
			}
			Long l = Long.valueOf(noOfLines);
			int count = l.intValue();
			System.out.println("COUNT OF LINES: " + count);

			File inputFile = new File(Constants.filePath);
			File tempFile = new File(inputFile.getAbsolutePath() + ".tmp");
			BufferedReader br = new BufferedReader(new FileReader(Constants.filePath));
			PrintWriter pw = new PrintWriter(new FileWriter(tempFile));
			String line = null;
			int index = 0;
			while ((line = br.readLine()) != null) {
				if (!line.isEmpty()) {
					pw.write(line);
					if (index < count - 1) {
						pw.write("\n");
					}
				}
				index++;
			}
			pw.close();
			br.close();
			if (!inputFile.delete()) {
				System.out.println("Could not delete file");
				return;
			}
			if (!tempFile.renameTo(inputFile))
				System.out.println("Could not rename file");

		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	void deleteEmpRecordInFile(int id, String name) {
		try {
			String lineToDelete = id + "," + name;
			File inputFile = new File(Constants.filePath);
			File tempFile = new File(inputFile.getAbsolutePath() + ".tmp");
			BufferedReader br = new BufferedReader(new FileReader(Constants.filePath));
			PrintWriter pw = new PrintWriter(new FileWriter(tempFile));
			String line = null;
			while ((line = br.readLine()) != null) {
				if (!line.trim().equals(lineToDelete)) {
					pw.println(line);
					pw.flush();
				}
			}
			pw.close();
			br.close();
			if (!inputFile.delete()) {
				System.out.println("Could not delete file");
				return;
			}
			if (!tempFile.renameTo(inputFile))
				System.out.println("Could not rename file");
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	List<Employee> getEmpsFromFile() {
		List<Employee> employees = new ArrayList<Employee>();
		File file = new File(Constants.filePath);
		FileReader fr = null;
		try {
			fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String line;
			while ((line = br.readLine()) != null) {
				List<String> content = Arrays.asList(line.split(","));
				Employee e = new Employee(Integer.parseInt(content.get(0)), content.get(1));
				employees.add(e);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				fr.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return employees;
	}
}
