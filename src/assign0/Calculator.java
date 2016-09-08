package assign0;

//Name: Luah Bao Jun (A0126258A)
//Assignment 0 Exercise 2

import java.util.*;

public class Calculator {

	private static final String ERROR_MESSAGE = "Error in expression";
	private static final String OPERATOR_ADD = "+";
	private static final String OPERATOR_MINUS = "-";
	private static final String OPERATOR_TIMES = "*";
	private static final String OPERATOR_DIVIDE = "/";

	private static final int MAX_SIZE = 3;

	private String operandA, operandB, operator;

	public Calculator() {

	}

	public Calculator(String operandA, String operandB, String operator) {
		this.operandA = operandA;
		this.operandB = operandB;
		this.operator = operator;
	}

	private void run() {
		try {
			calculate(Integer.parseInt(operandA), Integer.parseInt(operandB));
		} catch (NumberFormatException e) {
			// (2) Error if number given is not integer
			System.out.println(ERROR_MESSAGE);
		}
	}

	private void calculate(int operandA, int operandB) {

		boolean hasError = false;
		int ans = 0;

		switch(operator) {
			case OPERATOR_ADD:
				ans = operandA + operandB;
				break;
			case OPERATOR_MINUS:
				ans = operandA - operandB;
				break;
			case OPERATOR_TIMES:
				ans = operandA * operandB;
				break;
			case OPERATOR_DIVIDE:
				try {
					ans = operandA / operandB;
				} catch (ArithmeticException e) {
					// (3) Error due to division by zero
					hasError = true;
					System.out.println(ERROR_MESSAGE);
				}
				break;
			default:
				hasError = true;
				System.out.println(ERROR_MESSAGE);
				break;
		}

		if(!hasError) {
			System.out.println(operandA + " " + operator + " " + operandB + " = " + ans);
		}
	}

	public static void main(String[] args) {
		try {
			Calculator calculator = new Calculator(args[0], args[2], args[1]);
			calculator.run();
		} catch (ArrayIndexOutOfBoundsException e) {
			// (1) Error if there is no 2 operands and 1 operator (Length: 3)
			System.out.println(ERROR_MESSAGE);
		}
	}
}