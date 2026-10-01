package com.project.agriculturalblogapplication.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.project.agriculturalblogapplication.constatnt.AppConstants;
import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.LocalizedText;
import com.project.agriculturalblogapplication.enums.AscOrDescType;
import com.project.agriculturalblogapplication.payloads.PaginationArgs;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import java.beans.FeatureDescriptor;
import java.io.*;
import java.lang.reflect.Field;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class CommonUtils {

	private CommonUtils() {}

	public static boolean isTrue (Boolean value) {
		return value != null && value;
	}

	public static boolean isNotNullOrEmpty (String str) {
		return str != null && !str.trim().isEmpty();
	}

	public static boolean isNullOrEmpty (String str) {
		return str == null || str.trim().isEmpty();
	}

	public static boolean isNullOrEmptyList (List<?> values) {
		return values == null || values.isEmpty();
	}

	public static boolean isNotNullOrEmptyList (List<?> values) {
		return values != null && !values.isEmpty();
	}

	public static boolean isNotNullAndGreaterZero (Long id) {
		return id != null && id > 0;
	}

	public static boolean isValidPhoneNumber(String phoneNumber) {
		//String regex = "^[0-9]*$";
		String regex = "^[0][1-9]\\d{9}$|^[1-9]\\d{9}$";
		return phoneNumber.matches(regex);
	}

	public static boolean isValidUsername(String username) {
		String regex = "^[A-Za-z0-9@_.]+$";
		return username.matches(regex);
	}

	public static boolean isValidMail(String email) {
		String regex = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$";
		return email.matches(regex);
	}

	public static String getMaskedMobileNumber(String mobileNumber) {
		if (mobileNumber == null || mobileNumber.length() <= 6) {
			return mobileNumber;
		}

		String countryCode = mobileNumber.substring(0, 3);
		String maskedPart = mobileNumber.substring(2, mobileNumber.length()-3).replaceAll("\\d", "*");
		String suffix = mobileNumber.substring(mobileNumber.length()-3);
		return countryCode.concat(maskedPart).concat(suffix);
	}

	public static String getInvalidPasswordMessage(String password) {
		if(password == null || password.isBlank()) {
			return ErrorCode.ERROR_PASSWORD_IS_REQUIRED;
		}

		if(password.contains(" ")) {
			return ErrorCode.ERROR_PASSWORD_SHOULD_NOT_CONTAIN_WHITE_SPACE;
		}

		if(password.length() <= 7 || password.length() > 16) {
			return ErrorCode.ERROR_PASSWORD_MUST_HAVE_MINIMUM_OF_8_CHARACTERS_AND_MAXIMUM_OF_16_CHARACTERS;
		}

		Pattern specialAndDigitPattern = Pattern.compile("^(?=.*[a-zA-Z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\W]).{8,}$");
		boolean isMatched = specialAndDigitPattern.matcher(password).matches();

		if(!isMatched) {
			return ErrorCode.ERROR_PASSWORD_MUST_HAVE_A_DIGIT_A_SPECIAL_CHARACTER_AND_AN_ALPHABET;
		}

		return null;
	}


	public static String readFileToString(String path) {
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		Resource resource = resourceLoader.getResource(path);
		return asString(resource);
	}

	public static String asString(Resource resource) {
		try (Reader reader = new InputStreamReader(resource.getInputStream())) {
			return FileCopyUtils.copyToString(reader);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static LocalDateTime convertOffsetDateTimeToLocalDateTime(OffsetDateTime offsetDateTime) {
		if (offsetDateTime == null) return null;
		return offsetDateTime.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
	}

	public static Date addSecondsToJavaUtilDate(Date date, int seconds) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.SECOND, seconds);
		return calendar.getTime();
	}

	public static String getRandomStringWith(int length) {

		String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
				+ "0123456789"
				+ "abcdefghijklmnopqrstuvxyz";

		StringBuilder sb = new StringBuilder(length);

		for (int i = 0 ; i < length ; i++) {
			int index = (int)(AlphaNumericString.length() * Math.random());
			sb.append(AlphaNumericString
					.charAt(index));
		}
		return sb.toString();
	}

	public static int getRandomNumber(int min, int max){
		Random random = new Random();
		return random.ints(min,(max+1)).findFirst().getAsInt();
	}

	public static int getRandomNumberForOtp() {
		int min = 1000;
		int max = 9999;
        return getRandomNumber(min, max);
	}

	public static String formatStringToJson(String response) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			Object jsonObject = objectMapper.readValue(response, Object.class);
			ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
			return writer.writeValueAsString(jsonObject);
		} catch (Exception e) {
			return response;
		}
	}


	public static String loggerInfoText(String infoText){
		return "\u001B[34m" + infoText + "\u001B[0m";
	}

	public static String loggerDebugText(String debugText){
		return "\u001B[32m" + debugText + "\u001B[0m";
	}

	public static String loggerErrorText(String errorText){
		return "\u001B[31m" + errorText + "\u001B[0m";
	}

	public static String generateSlugFromString(String value){
		Pattern nonLatin = Pattern.compile("[^\\w-]");
		Pattern specialCharacters = Pattern.compile("[^a-zA-Z0-9 ]");
		Pattern whiteSpace = Pattern.compile("[\\s]");
		Pattern others = Pattern.compile("(^-|-$)");
		Pattern multiSpace = Pattern.compile("\\s{2,}");

		value = value.trim();

		Matcher matcher = multiSpace.matcher(value);

		value = specialCharacters.matcher(value).replaceAll("");
		value = multiSpace.matcher(value).replaceAll(" ");
		value = whiteSpace.matcher(value).replaceAll("-");
		value = Normalizer.normalize(value, Normalizer.Form.NFD);
		value = nonLatin.matcher(value).replaceAll("");
		value = others.matcher(value).replaceAll("");

		return value.toLowerCase(Locale.ENGLISH);
	}

	public static String makeSlug(String slug){
		char ch = '-';
		slug = slug.replace(' ',ch);
		return slug;
	}

	public static File convertMultipartToFile(MultipartFile file) throws IOException {
		File convertedFile = new File(file.getOriginalFilename());
		FileOutputStream fos = new FileOutputStream(convertedFile);
		fos.write(file.getBytes());
		fos.close();

		return convertedFile;
	}


	public static String[] getNullPropertyNames(Object source) {
		final BeanWrapper wrappedSource = new BeanWrapperImpl(source);
		return Stream.of(wrappedSource.getPropertyDescriptors())
				.map(FeatureDescriptor::getName)
				.filter(propertyName -> wrappedSource.getPropertyValue(propertyName) == null)
				.toArray(String[]::new);
	}

	public static Pageable getPageable(PaginationArgs paginationArgs) {
		Pageable pageable;
		String sortBy = paginationArgs.getSortBy();
		int pageNo = paginationArgs.getPageNo();
		int pageSize = paginationArgs.getPageSize();

		if(sortBy != null && sortBy.length() > 0) {
			if (paginationArgs.getAscOrDesc().equals(AscOrDescType.asc)) {
				pageable = PageRequest.of(pageNo, pageSize, Sort.by(sortBy).ascending());
			} else {
				pageable = PageRequest.of(pageNo, pageSize, Sort.by(sortBy).descending());
			}
		} else {
			pageable = PageRequest.of(pageNo, pageSize);
		}

		return pageable;
	}

	public static Pageable getPageable(int pageNo, int pageSize, String sortBy, AscOrDescType ascOrDesc) {
		Pageable pageable;

		if(sortBy != null && sortBy.trim().length() > 0) {
			if (ascOrDesc.equals(AscOrDescType.asc)) {
				pageable = PageRequest.of(pageNo, pageSize, Sort.by(sortBy).ascending());
			} else {
				pageable = PageRequest.of(pageNo, pageSize, Sort.by(sortBy).descending());
			}
		} else {
			pageable = PageRequest.of(pageNo, pageSize);
		}

		return pageable;
	}

	public static Pageable getLocalizedPageable(PaginationArgs paginationArgs, Class<?> className) {
		Pageable pageable;
		String sortBy = paginationArgs.getSortBy();
		int pageNo = paginationArgs.getPageNo();
		int pageSize = paginationArgs.getPageSize();

		try {
			Field field = className.getDeclaredField(sortBy);
			if (field.getType() == LocalizedText.class) {
				sortBy = sortBy+".translations.translatedText";
			}
		} catch (Exception ex) {
			sortBy = sortBy;
		}

		if (sortBy != null && sortBy.length() > 0) {
			if (paginationArgs.getAscOrDesc().equals(AscOrDescType.asc)) {
				pageable = PageRequest.of(pageNo, pageSize, Sort.by(sortBy).ascending());
			} else {
				pageable = PageRequest.of(pageNo, pageSize, Sort.by(sortBy).descending());
			}
		} else {
			pageable = PageRequest.of(pageNo, pageSize);
		}

		return pageable;
	}

	public static Map<String, Object> getParameters(Map<String, Object> parameters) {
		parameters.remove(AppConstants.PAGE_NO);
		parameters.remove(AppConstants.PAGE_SIZE);
		parameters.remove(AppConstants.SORT_BY);
		parameters.remove(AppConstants.ASC_OR_DESC);
		parameters.remove(AppConstants.PARAMETERS);
		parameters.remove(AppConstants.LANG);
		parameters.remove(AppConstants.LOAD_ORIGINAL_TEXT);
		return parameters;
	}

	public static <D, E> D mapEntityToDto(ModelMapper modelMapper, E entity, Class<D> dtoClass) {
		return modelMapper.map(entity, dtoClass);
	}

	public static <E, D> Page<D> mapEntityPageToDtoPage(ModelMapper modelMapper, Page<E> entities, Class<D> dtoClass) {
		return entities.map(objectEntity -> modelMapper.map(objectEntity, dtoClass));
	}
}
