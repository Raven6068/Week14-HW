package zerobase.weather.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import zerobase.weather.domain.DateWeather;
import zerobase.weather.domain.Diary;
import zerobase.weather.repository.DateWeatherRepository;
import zerobase.weather.repository.DiaryRepository;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final DateWeatherRepository dateWeatherRepository;

    @Value("${openweathermap.key}")
    private String apiKey;

    private static final String WEATHER_URL =
            "https://api.openweathermap.org/data/2.5/weather?q=Seoul&appid=%s&units=metric&lang=kr";

    // =================== Create ===================
    @Transactional
    public void createDiary(LocalDate date, String text) {
        DateWeather dateWeather = getDateWeather(date);

        Diary diary = new Diary();
        diary.setDate(date);
        diary.setWeather(dateWeather.getWeather());
        diary.setIcon(dateWeather.getIcon());
        diary.setTemperature(dateWeather.getTemperature());
        diary.setText(text);

        diaryRepository.save(diary);
    }

    // =================== Read =====================
    @Transactional(readOnly = true)
    public List<Diary> readDiary(LocalDate date) {
        return diaryRepository.findAllByDate(date);
    }

    @Transactional(readOnly = true)
    public List<Diary> readDiaries(LocalDate startDate, LocalDate endDate) {
        return diaryRepository.findAllByDateBetween(startDate, endDate);
    }

    // =================== Update ===================
    @Transactional
    public void updateDiary(LocalDate date, String text) {
        List<Diary> diaries = diaryRepository.findAllByDate(date);
        if (diaries.isEmpty()) {
            throw new RuntimeException("해당 날짜의 일기가 없습니다.");
        }
        Diary diary = diaries.get(0);
        diary.setText(text);
    }

    // =================== Delete ===================
    @Transactional
    public void deleteDiary(LocalDate date) {
        diaryRepository.deleteAllByDate(date);
    }

    // =================== Weather ==================
    private DateWeather getDateWeather(LocalDate date) {
        List<DateWeather> dateWeathers = dateWeatherRepository.findAllByDate(date);
        if (!dateWeathers.isEmpty()) {
            return dateWeathers.get(0);
        }

        DateWeather dateWeather = getWeatherFromApi(date);
        dateWeatherRepository.save(dateWeather);
        return dateWeather;
    }

    private DateWeather getWeatherFromApi(LocalDate date) {
        String json = getWeatherJson();

        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(json);

            JSONObject main = (JSONObject) root.get("main");
            double temperature = ((Number) main.get("temp")).doubleValue();

            JSONArray weatherArr = (JSONArray) root.get("weather");
            JSONObject weather = (JSONObject) weatherArr.get(0);

            String mainWeather = (String) weather.get("main");
            String icon = (String) weather.get("icon");

            return new DateWeather(date, mainWeather, icon, temperature);

        } catch (ParseException e) {
            log.error("날씨 데이터 파싱 실패", e);
            throw new RuntimeException("날씨 데이터 파싱 실패", e);
        }
    }

    private String getWeatherJson() {
        RestTemplate restTemplate = new RestTemplate();
        URI uri = URI.create(String.format(WEATHER_URL, apiKey));
        return restTemplate.getForObject(uri, String.class);
    }

    // =================== Scheduler =================
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void saveTodayWeather() {
        LocalDate today = LocalDate.now();
        DateWeather dateWeather = getWeatherFromApi(today);
        dateWeatherRepository.save(dateWeather);
    }
}
