package zerobase.weather.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DateWeather {

    @Id
    private LocalDate date;

    private String weather;

    private String icon;

    private double temperature;
}
