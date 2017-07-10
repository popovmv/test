import com.sun.javafx.scene.layout.region.Margins;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Created by mvpopov on 7/9/17.
 */
public class Solution {


    private static final String INPUT =
            "test1.jpg, Odessa, 2015-09-05 12:20:30"
                    + FileSplitter.LINE_SEPARATOR
                    + "test2.png, Odessa, 2015-09-05 13:20:31"
                    + FileSplitter.LINE_SEPARATOR
                    + "test3.png, Odessa, 2015-09-05 13:20:32"
                    + FileSplitter.LINE_SEPARATOR
                    + "test4.png, Odessa, 2015-09-05 13:20:33"
                    + FileSplitter.LINE_SEPARATOR
                    + "test5.png, Odessa, 2015-09-05 13:20:34"
                    + FileSplitter.LINE_SEPARATOR
                    + "test6.png, Odessa, 2015-09-05 13:20:35"
                    + FileSplitter.LINE_SEPARATOR
                    + "test7.png, Odessa, 2015-09-05 13:20:36"
                    + FileSplitter.LINE_SEPARATOR
                    + "test8.png, Odessa, 2015-09-05 13:20:37"
                    + FileSplitter.LINE_SEPARATOR
                    + "test9.png, Odessa, 2015-09-05 13:20:39"
                    + FileSplitter.LINE_SEPARATOR
                    + "test10.png, Odessa, 2015-09-05 13:20:39"
                    + FileSplitter.LINE_SEPARATOR
                    + "test11.jpg, Lviv, 2015-10-05 14:20:30"
                    + FileSplitter.LINE_SEPARATOR
                    + "test12.png, Odessa, 2015-09-05 13:20:40"
                    + FileSplitter.LINE_SEPARATOR
                    + "test4.jpg, Donetsk, 2015-11-05 15:20:30";


    public static void main(String[] args) {
        System.out.print(new StringPhotoConverter().convert(INPUT));
    }
}

interface Converter<F, T> {
    T convert(F from);
}

class StringPhotoConverter implements Converter<String, String> {
    private static final Splitter PHOTO_SPLITTER = new FileSplitter();
    private static final Splitter PHOTO_DETAILS_SPLITTER = new PhotoSplitter();
    private static final Converter<String[], Photo> TO_PHOTO_CONVERTER = new ArrayToPhotoConverter();

    @Override
    public String convert(String from) {
        List<Photo> photos = Stream.of(PHOTO_SPLITTER.split(from))
                .map(item -> PHOTO_DETAILS_SPLITTER.split(item))
                .map(item -> TO_PHOTO_CONVERTER.convert(item))
                .collect(Collectors.toList());

        updateNumber(photos);

        final StringBuilder convertedString = new StringBuilder();
        photos.forEach(item->convertedString.append(item).append(FileSplitter.LINE_SEPARATOR));
        return convertedString.toString();
    }

    private static void updateNumber(final List<Photo> photos) {
        final Map<String, Long> photosPerCity = calculatePhotosPerCity(photos);
        final Map<String, AtomicInteger> counter = createCounter(photos);

        PhotoFormatter formatter = new NumberFormatter(calculatePhotosPerCity(photos), createCounter(photos));
        photos.stream().forEach(item -> {
            item.setNumber(formatter.format(item.getCity()));
        });
    }

    private static Map<String, Long> calculatePhotosPerCity(final List<Photo> photos) {
        return photos.stream().map(item -> item.getCity())
                .collect(
                        Collectors.groupingBy(
                                Function.identity(), Collectors.counting()
                        )
                );
    }

    private static Map<String, AtomicInteger> createCounter(final List<Photo> photos) {
        final Map<String, AtomicInteger> counter = new HashMap<>();
        photos.stream().forEach(item -> counter.putIfAbsent(item.getCity(), new AtomicInteger(0)));
        return counter;
    }
}

class ArrayToPhotoConverter implements Converter<String[], Photo> {
    private static final Retriever FILE_NAME_RETRIEVER = new FileNameRetriever();
    private static final Retriever EXTENSION_RETRIEVER = new ExtensionRetriever();
    private static final Retriever CITY_NAME_RETRIEVER = new CityNameRetriever();
    private static final Retriever DATE_TIME_NAME_RETRIEVER = new DateTimeRetriever();

    private static final Converter<String, LocalDateTime> DATE_TIME_CONVERTER = new Converter<String, LocalDateTime>() {
        private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
        @Override
        public LocalDateTime convert(String from) {
            return LocalDateTime.parse(from, DateTimeFormatter.ofPattern
                    (DATE_TIME_FORMAT));
        }
    };

    private static final PhotoFormatter CITY_NAME_FORMATTER = new PhotoFormatter() {};

    public Photo convert(String[] values) {
        String fileNameValue = FILE_NAME_RETRIEVER.retrieve(values);
        String extensionValue = EXTENSION_RETRIEVER.retrieve(values);
        String cityNameValue = CITY_NAME_RETRIEVER.retrieve(values);
        String dateTimeValue = DATE_TIME_NAME_RETRIEVER.retrieve(values);

        LocalDateTime dateTime = DATE_TIME_CONVERTER.convert(dateTimeValue);

        Photo photo = new Photo();
        photo.setName(fileNameValue);
        photo.setCity(CITY_NAME_FORMATTER.format(cityNameValue));
        photo.setDateTime(dateTime);
        photo.setExtension(Extension.getValue(extensionValue));

        return photo;
    }
}

interface PhotoFormatter {
    default String format(String value) {
        String lowerCaseValue = value.toLowerCase();
        return lowerCaseValue.substring(0, 1).toUpperCase() + lowerCaseValue.substring(1);
    }
}

class NumberFormatter implements PhotoFormatter {
    private final Map<String, Long> photosPerCity;
    private final Map<String, AtomicInteger> counter;

    public NumberFormatter(Map<String, Long> photosPerCity, Map<String, AtomicInteger> counter) {
        this.photosPerCity = photosPerCity;
        this.counter = counter;
    }

    @Override
    public String format(String value) {
        long digits = calculateDigits(this.photosPerCity.get(value).intValue());
        int index = counter.get(value).addAndGet(1);

        return String.format("%0" + digits + "d", index);
    }

    private static long calculateDigits(double value) {
        return (long) (Math.log10(value) + 1);
    }
}

class Photo {
    private String name;
    private Extension extension;
    private String city;
    private LocalDateTime dateTime;
    private String number;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Extension getExtension() {
        return extension;
    }

    public void setExtension(Extension extension) {
        this.extension = extension;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Photo photo = (Photo) o;
        return Objects.equals(name, photo.name) &&
                extension == photo.extension &&
                Objects.equals(city, photo.city) &&
                Objects.equals(dateTime, photo.dateTime) &&
                Objects.equals(number, photo.number);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, extension, city, dateTime, number);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Photo{");
        sb.append("name='").append(name).append('\'');
        sb.append(", extension=").append(extension);
        sb.append(", city='").append(city).append('\'');
        sb.append(", dateTime=").append(dateTime);
        sb.append(", number='").append(number).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

enum Extension {
    JPEG("jpeg"),
    JPG("jpg"),
    PNG("png");

    private final String value;

    Extension(String value) {
        this.value = value;
    }

    public static Extension getValue(String value) {
        for (Extension extension : Extension.values()) {

            if (extension.value.equalsIgnoreCase(value)) {
                return extension;
            }
        }
        throw new IllegalArgumentException();
    }
}


interface Retriever {
    String retrieve(String[] values);
}

class FileNameRetriever implements Retriever {
    @Override
    public String retrieve(String[] values) {
        String rawFileName = values[0].trim();
        int pos = rawFileName.indexOf(".", -1);
        return rawFileName.substring(0, pos);
    }
}

class ExtensionRetriever implements Retriever {
    @Override
    public String retrieve(String[] values) {
        String rawFileName = values[0].trim();
        int pos = rawFileName.trim().indexOf(".", -1);
        return rawFileName.substring(pos + 1, rawFileName.length());
    }
}

class CityNameRetriever implements Retriever {
    @Override
    public String retrieve(String[] values) {
        String cityName = values[1].trim();
        return cityName.toLowerCase();
    }
}

class DateTimeRetriever implements Retriever {
    @Override
    public String retrieve(String[] values) {
        String rawDateTimeName = values[2];
        return rawDateTimeName.trim();
    }
}

interface Splitter {
    String[] split(String string);
}

class FileSplitter implements Splitter {
    static final String LINE_SEPARATOR = System.getProperty("line.separator");

    public String[] split(String string) {
        return string.split(LINE_SEPARATOR);
    }
}

class PhotoSplitter implements Splitter {
    private static final String DETAILS_SEPARATOR = ",";

    public String[] split(String string) {
        return string.split(DETAILS_SEPARATOR);
    }
}
