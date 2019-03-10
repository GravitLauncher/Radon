package me.itzsomebody.radon.exceptions;

public class IllegalConfigurationValueException extends RuntimeException {
	private static final long serialVersionUID = -1489040983823481321L;

	public IllegalConfigurationValueException(String msg) {
        super(msg);
    }

    @SuppressWarnings("rawtypes")
	public IllegalConfigurationValueException(String value, Class expected, Class gotInstead) {
        super(String.format("Value %s was expected to be %s, got %s instead.", value, expected.getName(),
                gotInstead.getName()));
    }
}
