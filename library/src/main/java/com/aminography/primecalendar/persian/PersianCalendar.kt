package com.aminography.primecalendar.persian

import com.aminography.primecalendar.base.BaseCalendar
import com.aminography.primecalendar.civil.CivilCalendar
import com.aminography.primecalendar.common.CalendarType
import com.aminography.primecalendar.common.DateHolder
import com.aminography.primecalendar.common.convertPersianToCivil
import com.aminography.primecalendar.common.convertPersianToHijri
import com.aminography.primecalendar.hijri.HijriCalendar
import java.util.Calendar.*

/**
 * @author aminography
 */
class PersianCalendar : BaseCalendar() {

    private var persianYear: Int = 0
    private var persianMonth: Int = 0
    private var persianDayOfMonth: Int = 0

    override var year: Int
        get() = persianYear
        set(value) {
            set(value, persianMonth, persianDayOfMonth)
        }

    override var month: Int
        get() = persianMonth
        set(value) {
            set(persianYear, value, persianDayOfMonth)
        }

    override var dayOfMonth: Int
        get() = persianDayOfMonth
        set(value) {
            set(persianYear, persianMonth, value)
        }

    override val monthName: String
        get() = PersianCalendarUtils.persianMonthNames[persianMonth]

    override val weekDayName: String
        get() = when (get(DAY_OF_WEEK)) {
            SATURDAY -> PersianCalendarUtils.persianWeekDays[0]
            SUNDAY -> PersianCalendarUtils.persianWeekDays[1]
            MONDAY -> PersianCalendarUtils.persianWeekDays[2]
            TUESDAY -> PersianCalendarUtils.persianWeekDays[3]
            WEDNESDAY -> PersianCalendarUtils.persianWeekDays[4]
            THURSDAY -> PersianCalendarUtils.persianWeekDays[5]
            else -> PersianCalendarUtils.persianWeekDays[6]
        }

    override val monthLength: Int
        get() = PersianCalendarUtils.monthLength(year, month)

    override val isLeapYear: Boolean
        get() = PersianCalendarUtils.isPersianLeapYear(year)

    override var firstDayOfWeek: Int = SATURDAY
        set(value) {
            field = value
            setInternalFirstDayOfWeek(value)
        }

    override val calendarType: CalendarType
        get() = CalendarType.PERSIAN

    init {
        invalidate()
        setInternalFirstDayOfWeek(firstDayOfWeek)
    }

    // ---------------------------------------------------------------------------------------------

    override fun get(field: Int): Int {
        return when (field) {
            ERA -> super.get(ERA)
            YEAR -> year
            MONTH -> month
            WEEK_OF_YEAR -> calculateWeekOfYear()
            WEEK_OF_MONTH -> calculateWeekOfMonth()
            DAY_OF_MONTH -> dayOfMonth // also DATE
            DAY_OF_YEAR -> calculateDayOfYear()
            DAY_OF_WEEK -> super.get(DAY_OF_WEEK)
            DAY_OF_WEEK_IN_MONTH -> throw NotImplementedError("DAY_OF_WEEK_IN_MONTH is not implemented yet!")
            else -> super.get(field)
        }
    }

    override fun add(field: Int, amount: Int) {
        if (amount == 0) {
            return
        }
        if (field < 0 || field >= ZONE_OFFSET) {
            throw IllegalArgumentException()
        }

        when (field) {
            YEAR -> set(persianYear + amount, persianMonth, persianDayOfMonth)
            MONTH -> {
                if (amount > 0) {
                    set(persianYear + (persianMonth + amount) / 12, (persianMonth + amount) % 12, persianDayOfMonth)
                } else {
                    set(persianYear - (12 - (persianMonth + amount + 1)) / 12, (12 + (persianMonth + amount)) % 12, persianDayOfMonth)
                }
            }
            else -> {
                super.add(field, amount)
                invalidate()
            }
        }
    }

    override fun set(field: Int, value: Int) {
        if (value < 0) {
            throw IllegalArgumentException()
        }
        if (field < 0 || field >= ZONE_OFFSET) {
            throw IllegalArgumentException()
        }
        when (field) {
            ERA -> {
                super.set(field, value)
                invalidate()
            }
            YEAR -> {
                year = value
            }
            MONTH -> {
                month = value
            }
            DAY_OF_MONTH -> { // also DATE
                dayOfMonth = value
            }
            WEEK_OF_YEAR -> {
                val firstDayOfYear = PersianCalendar().also {
                    it.set(year, 0, 1)
                }
                val firstDayOfYearDayOfWeek = firstDayOfYear.get(DAY_OF_WEEK)
                val currentDayOfWeek = weekOffsetFromFirstDayOfWeek(get(DAY_OF_WEEK))

                val move = (value - 1) * 7 + (currentDayOfWeek - firstDayOfYearDayOfWeek)
                firstDayOfYear.add(DAY_OF_YEAR, move)
                firstDayOfYear.let {
                    set(it.year, it.month, it.dayOfMonth)
                }
            }
            WEEK_OF_MONTH -> {
                val firstDayOfMonth = PersianCalendar().also {
                    it.set(year, month, 1)
                }
                val firstDayOfMonthDayOfWeek = firstDayOfMonth.get(DAY_OF_WEEK)
                val currentDayOfWeek = weekOffsetFromFirstDayOfWeek(get(DAY_OF_WEEK))

                val move = (value - 1) * 7 + (currentDayOfWeek - firstDayOfMonthDayOfWeek)
                firstDayOfMonth.add(DAY_OF_YEAR, move)
                firstDayOfMonth.let {
                    set(it.year, it.month, it.dayOfMonth)
                }
            }
            DAY_OF_YEAR -> {
                if (value > PersianCalendarUtils.yearLength(year)) {
                    throw IllegalArgumentException()
                } else {
                    PersianCalendarUtils.dayOfYear(year, value).let {
                        set(it.year, it.month, it.dayOfMonth)
                    }
                }
            }
            DAY_OF_WEEK -> {
                super.set(field, value)
                invalidate()
            }
            DAY_OF_WEEK_IN_MONTH -> throw NotImplementedError("DAY_OF_WEEK_IN_MONTH is not implemented yet!")
            else -> {
                super.set(field, value)
                invalidate()
            }
        }
    }

    override fun set(year: Int, month: Int, dayOfMonth: Int) {
        persianYear = year
        persianMonth = month
        persianDayOfMonth = dayOfMonth

        PersianCalendarUtils.persianToGregorian(
                DateHolder(persianYear, persianMonth, persianDayOfMonth)
        ).let {
            super.set(it.year, it.month, it.dayOfMonth)
        }
    }

    override fun set(year: Int, month: Int, dayOfMonth: Int, hourOfDay: Int, minute: Int) {
        set(year, month, dayOfMonth)
        super.set(HOUR_OF_DAY, hourOfDay)
        super.set(MINUTE, minute)
    }

    override fun set(year: Int, month: Int, dayOfMonth: Int, hourOfDay: Int, minute: Int, second: Int) {
        set(year, month, dayOfMonth)
        super.set(HOUR_OF_DAY, hourOfDay)
        super.set(MINUTE, minute)
        super.set(SECOND, second)
    }

    override fun invalidate() {
        PersianCalendarUtils.gregorianToPersian(
                DateHolder(
                        super.get(YEAR),
                        super.get(MONTH),
                        super.get(DAY_OF_MONTH)
                )
        ).let {
            persianYear = it.year
            persianMonth = it.month
            persianDayOfMonth = it.dayOfMonth
        }
    }

    // ---------------------------------------------------------------------------------------------

    override fun calculateDayOfYear(): Int = PersianCalendarUtils.dayOfYear(year, month, dayOfMonth)

    // ---------------------------------------------------------------------------------------------

    override fun toCivil(): CivilCalendar = convertPersianToCivil(this)

    override fun toPersian(): PersianCalendar = this

    override fun toHijri(): HijriCalendar = convertPersianToHijri(this)

}