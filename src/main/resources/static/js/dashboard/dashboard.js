document.addEventListener('DOMContentLoaded', function () {

    const COLOR_INCOME = '#2D6A4F';
    const COLOR_EXPENSE = '#C1604A';
    const DONUT_COLORS = ['#C1604A', '#D4846F', '#E4A898', '#EFC8BC', '#F5E2DC', '#cbd5e1'];

    const won = (n) => '₩' + Math.round(n).toLocaleString('ko-KR');
    const signedWon = (n) => (n >= 0 ? '+' : '−') + won(Math.abs(n));

    const today = new Date();
    let selectedYear = today.getFullYear();
    let selectedMonth = today.getMonth() + 1; // Date는 0-based, 서버 API는 1-based라 여기서 바로 맞춰둠

    // 월을 바꿀 때마다 다시 render()하면 같은 컨테이너에 차트가 중첩되므로 인스턴스를 들고 있다가 매번 destroy 후 재생성
    let barChart = null;
    let donutChart = null;

    initDatePicker();
    loadAll();

    // <input type="month">는 화면에 안 보이게 숨겨두고, pill 클릭 시 showPicker()로 강제로 띄운다.
    // (input을 pill 위에 겹쳐서 클릭을 그대로 흘려보내는 방식은 브라우저가 달력 아이콘의 아주 좁은
    // 히트 영역을 클릭했을 때만 피커를 띄우기 때문에, 커진 pill 영역을 클릭하면 대부분 반응이 없었다)
    function initDatePicker() {
        const monthInput = document.querySelector('#month-picker-input');
        monthInput.value = toMonthInputValue(selectedYear, selectedMonth);

        document.querySelector('#date-pill').addEventListener('click', () => {
            try {
                monthInput.showPicker();
            } catch (e) {
                monthInput.focus();
            }
        });

        monthInput.addEventListener('change', () => {
            if (!monthInput.value) {
                return;
            }
            const [year, month] = monthInput.value.split('-').map(Number);
            selectedYear = year;
            selectedMonth = month;
            updateDatePillText();
            loadAll();
        });
        updateDatePillText();
    }

    function toMonthInputValue(year, month) {
        return `${year}-${String(month).padStart(2, '0')}`;
    }

    function updateDatePillText() {
        document.querySelector('#date-pill-text').textContent = `${selectedYear}년 ${selectedMonth}월`;
    }

    function loadAll() {
        loadSummary();
        loadMonthlyTrend();
        loadCategoryExpense();
        loadRecentTransactions();
    }

    function dashboardQuery() {
        return `year=${selectedYear}&month=${selectedMonth}`;
    }

    // KPI 카드(선택한 달 수입/지출/현재 잔액) - 잔액은 거래 합계로 역산할 수 없는 독립 스냅샷이라
    // 전월 대비 배지 없이 현재 값만 보여준다.
    function loadSummary() {
        fetch(`/api/dashboard/summary?${dashboardQuery()}`)
            .then((res) => res.json())
            .then((data) => {
                renderKpi('income', data.thisMonthIncome, data.lastMonthIncome);
                renderKpi('expense', data.thisMonthExpense, data.lastMonthExpense);
                document.querySelector('#kpi-balance-number').textContent = Math.round(data.totalBalance).toLocaleString('ko-KR');
            });
    }

    function renderKpi(key, current, previous) {
        document.querySelector(`#kpi-${key}-number`).textContent = Math.round(current).toLocaleString('ko-KR');

        const badge = document.querySelector(`#kpi-${key}-badge`);
        const diff = current - previous;

        if (previous === 0) {
            badge.style.display = 'none';
        } else {
            const rate = (diff / previous) * 100;
            badge.textContent = `${rate >= 0 ? '▲' : '▼'} ${Math.abs(rate).toFixed(1)}%`;
        }

        document.querySelector(`#kpi-${key}-sub`).textContent = `지난달 대비 ${signedWon(diff)}`;
    }

    // 월별 수입/지출 Bar 차트 - 최근 6개월
    function loadMonthlyTrend() {
        fetch(`/api/dashboard/monthly-trend?${dashboardQuery()}`)
            .then((res) => res.json())
            .then((rows) => {
                const barOptions = {
                    chart: {
                        type: 'bar',
                        height: 280,
                        toolbar: {show: false},
                        fontFamily: 'Pretendard Variable, sans-serif',
                    },
                    plotOptions: {
                        bar: {
                            columnWidth: '40%',
                            borderRadius: 4,
                        }
                    },
                    stroke: {
                        show: true,
                        width: 4, // 막대 사이의 미세한 간격을 벌려주는 흰색 테두리 효과
                        colors: ['transparent']
                    },
                    // 모바일에서는 컨테이너 폭이 줄어드는데 columnWidth 비율(40%)은 그대로라
                    // 6개월치 막대가 선처럼 얇아짐 - 폭을 넓히고 테두리도 얇춰서 보정
                    responsive: [
                        {
                            breakpoint: 767.98,
                            options: {
                                chart: {height: 240},
                                plotOptions: {bar: {columnWidth: '65%'}},
                                stroke: {width: 2},
                                xaxis: {labels: {style: {fontSize: '10px'}}},
                                yaxis: {labels: {style: {fontSize: '10px'}}}
                            }
                        }
                    ],
                    dataLabels: {enabled: false},
                    series: [
                        {name: '수입', data: rows.map((row) => row.income)},
                        {name: '지출', data: rows.map((row) => row.expense)}
                    ],
                    colors: [COLOR_INCOME, COLOR_EXPENSE],
                    xaxis: {
                        categories: rows.map((row) => `${parseInt(row.yearMonth.split('-')[1], 10)}월`),
                        axisBorder: {show: false},
                        axisTicks: {show: false},
                        labels: {style: {colors: '#5B6B7B', fontSize: '12px', fontWeight: 600}}
                    },
                    yaxis: {
                        min: 0,
                        tickAmount: 4,
                        labels: {
                            formatter: (val) => (val / 10000).toFixed(0) + '만',
                            style: {colors: '#a4afba', fontSize: '11px'}
                        }
                    },
                    fill: {
                        type: 'solid',
                        opacity: 1,
                    },
                    states: {
                        normal: {filter: {type: 'none', value: 0}},
                        hover: {filter: {type: 'none', value: 0}},
                        active: {allowMultipleDataPointsSelection: false, filter: {type: 'none', value: 0}}
                    },
                    grid: {
                        borderColor: '#eef1f5',
                        strokeDashArray: 0,
                        padding: {
                            left: 0,
                            right: 0
                        }
                    },
                    legend: {show: false},
                    tooltip: {y: {formatter: (val) => won(val)}}
                };

                if (barChart) {
                    barChart.destroy();
                }
                barChart = new ApexCharts(document.querySelector('#chart-monthly-bar'), barOptions);
                barChart.render();
            });
    }

    // 카테고리별 지출 도넛 차트 - 이번 달 지출 상위 5개 + 기타
    function loadCategoryExpense() {
        fetch(`/api/dashboard/category-expense?${dashboardQuery()}`)
            .then((res) => res.json())
            .then((rows) => {
                renderDonutLegend(rows);

                const totalExpense = rows.reduce((sum, row) => sum + row.amount, 0);
                const colors = rows.map((_, i) => DONUT_COLORS[i % DONUT_COLORS.length]);

                const donutOptions = {
                    chart: {
                        type: 'donut',
                        height: 220,
                        fontFamily: 'Pretendard Variable, sans-serif',
                    },
                    series: rows.map((row) => row.amount),
                    labels: rows.map((row) => row.categoryName),
                    colors: colors,
                    dataLabels: {enabled: false},
                    legend: {show: false},
                    plotOptions: {
                        pie: {
                            donut: {
                                size: '70%',
                                labels: {
                                    show: true,
                                    // 1. name(카테고리 라벨)의 스타일과 색상을 고정합니다.
                                    name: {
                                        show: true,
                                        fontSize: '14px',
                                        fontFamily: 'Pretendard Variable, sans-serif',
                                        fontWeight: 500,
                                        color: '#8a97a5', // 마우스 오버해도 이 색상으로 고정됩니다.
                                    },
                                    // 2. value(가운데 큰 숫자)의 스타일과 색상을 고정합니다.
                                    value: {
                                        show: true,
                                        fontSize: '20px',
                                        fontFamily: 'Pretendard Variable, sans-serif',
                                        fontWeight: 600,
                                        color: '#2c3e50', // 마우스 오버해도 이 색상으로 고정됩니다.
                                        formatter: (val) => val // 원래 숫자가 잘 나오도록 맵핑 (필요시 포맷터 적용)
                                    },
                                    total: {
                                        show: true,
                                        label: '총 지출',
                                        fontSize: '11px',
                                        fontWeight: 600,
                                        color: '#8a97a5',
                                        formatter: () => won(totalExpense)
                                    }
                                }
                            }
                        }
                    },
                    tooltip: {
                        enabled: true,
                        custom: function ({series, seriesIndex, dataPointIndex, w}) {
                            // 마우스 오버한 항목의 라벨명과 값을 가져옵니다.
                            const label = w.globals.labels[seriesIndex];
                            const value = series[seriesIndex].toLocaleString('ko-KR');

                            // 툴팁의 HTML 구조와 스타일을 직접 지정합니다.
                            return `
                              <div style="
                                background: #ffffff;
                                color: #2c3e50;
                                padding: 8px 12px;
                                border: 1px solid #e2e8f0;
                                border-radius: 6px;
                                box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
                                font-size: 12px;
                                font-weight: 500;
                              ">
                                <span style="font-weight: 700; color: #64748b;">${label}:</span>
                                <span>₩${value}</span>
                              </div>
                            `;
                        }
                    },
                    stroke: {width: 2, colors: ['#fff']}
                };

                if (donutChart) {
                    donutChart.destroy();
                }
                donutChart = new ApexCharts(document.querySelector('#chart-category-donut'), donutOptions);
                donutChart.render();
            });
    }

    function renderDonutLegend(rows) {
        const container = document.querySelector('#donut-legend');
        container.innerHTML = '';

        rows.forEach((row, i) => {
            const item = document.createElement('div');
            item.className = 'donut-legend-item';
            item.innerHTML = `
                <span class="donut-legend-dot" style="background:${DONUT_COLORS[i % DONUT_COLORS.length]};"></span>
                <span class="donut-legend-name">${row.categoryName}</span>
                <span class="donut-legend-pct">${row.percentage.toFixed(1)}%</span>
                <span class="donut-legend-amount">${won(row.amount)}</span>
            `;
            container.appendChild(item);
        });
    }

    // 최근 거래 목록 - 거래 자체에 별도 제목 필드가 없어 카테고리명을 이름으로 사용
    function loadRecentTransactions() {
        fetch(`/api/dashboard/recent-transactions?${dashboardQuery()}`)
            .then((res) => res.json())
            .then((rows) => {
                const container = document.querySelector('#recent-transaction-list');
                container.innerHTML = '';

                rows.forEach((tx) => {
                    const isIncome = tx.type === 'INCOME';
                    const date = new Date(tx.transactionDate);

                    const item = document.createElement('div');
                    item.className = 'transaction-item';
                    item.innerHTML = `
                        <div class="category-badge${isIncome ? ' income' : ''}">${tx.categoryName.charAt(0)}</div>
                        <div>
                            <div class="transaction-name">${tx.categoryName}</div>
                            <div class="transaction-meta">${isIncome ? '수입' : '지출'} · ${date.getMonth() + 1}월 ${date.getDate()}일</div>
                        </div>
                        <div class="transaction-amount ${isIncome ? 'text-income' : 'text-expense'}">${isIncome ? '+' : '−'}${won(tx.amount)}</div>
                    `;
                    container.appendChild(item);
                });
            });
    }

});
