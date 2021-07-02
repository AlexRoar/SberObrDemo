
function create_UUID() {
    // функция для генерации уникального идентификатора заказа (order_id) для сервиса платежей
    var dt = new Date().getTime();
    var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = (dt + Math.random()*16)%16 | 0;
        dt = Math.floor(dt/16);
        return (c=='x' ? r :(r&0x3|0x8)).toString(16);
    });
    return uuid;
}

// получаем описание по коду платежа
function getStatusDescription(code) {
    
    // https://developer.sberdevices.ru/docs/ru/payment_tools/smartpay_api#формат-ответа-1
    // https://developer.sberdevices.ru/docs/ru/payment_tools/smartapp_graph_jscode_payments/#запуск-сценария-оплаты-в-ассистенте
    switch (code) {
        case 0:
            return "успешная оплата";
        case 1:
            return "неожиданная ошибка";
        case 2:
            return "пользователь закрыл смартап";
        case 3:
            return "невозможно начать оплату, так как отображается другой сценарий оплаты";
        case 4:
            return "время оплаты счета истекло";
        case 5:
            return "оплата отклонена бэкендом";
        case "confirmed":
            return "оплачен (успешное завершение второй фазы двухстадийного платежа)";
        case "created":
            return "создан, ожидает выбора платежного инструмента";
        case "executed":
            return "находится в процессе оплаты";
        case "paid":
            return "захолдирован (сумма захолдирована при двухстадийном платеже)";
        case "cancelled":
            return "отменен пользователем (не продолжил оплату)";
        case "reversed":
            return "отменен продавцом (отмена холдирования при двухстадийной оплате)";
        case "refunded":
            return "осуществлен полный возврат";
        default:
            return "статус неизвестен";
    }
}

// используется для передачи кастомных ответов в нативное приложение
function reply(body, response) {
    var replyData = {
        type: "raw",
        body: body
    };
    response.replies = response.replies || [];
    response.replies.push(replyData);
}

// используется для передачи кастомных ответов в с кнопками
function replyWithButtons(response, myButtons) {
    var replyData = {
        type: "buttons",
        buttons: myButtons
    };
    response.replies = response.replies || [];
    response.replies.push(replyData);
}

// команда надевания одежды для нативного аппа
function formWearCommand(whatToWear) {
    return {
        "command": {
            type: "smart_app_data",
            smart_app_data: {
                command: "wear_this",
                clothes: whatToWear
            }
        }
    }
}

// команда удаления одежды
function formClearClothesCommand() {
    return {
        "command": {
            type: "smart_app_data",
            smart_app_data: {
                command: "dont_wear_anything"
            }
        }
    }
}

// команда успешной покупки слона
function formSuccessBuyCommand() {
    return {
        "command": {
            type: "smart_app_data",
            smart_app_data: {
                command: "buy_success",
                buyItems: [ "elephant" ]
            }
        }
    }
}

// команда неуспешной покупки слона
function formFailBuyCommand() {
    return {
        "command": {
            type: "smart_app_data",
            smart_app_data: {
                command: "buy_fail"
            }
        }
    }
}

// Отправка только озвучки - ассистент на клиенте только произносит фразу,
// но не показывает текст в виде бабла
function sendAnswer_Speech(text) {
    var response = $jsapi.context().response;
    var reply = {
        type: "raw",
        body: {
            "pronounceText": text
        }
    };
    response.replies = response.replies || [];
    response.replies.push(reply);
}

// Создаем обьект заказа
function createOrder(orderId, serviceId) {
    return {
        order: {                    
            //Идентификатор заказа для сервиса платежей.
            //Должен быть уникален в рамках выделенного для приложения service_id
            "order_id": orderId,
            // Идентификатор сервиса, выдается вместе с токеном
            "service_id": serviceId,
            // Номер заказа для пользователя в произвольном формате, необязательное поле
            "order_number": 1,
            //Сумма заказа, должна совпадать с суммой всех позиций
            "amount": 100,
            // Наименование вашего юридического лица
            "purpose": 'ОАО Торговец слонами',
            // Описание платежа для отображения пользователю
            "description": 'Покупка слона',
            // Система налогообложения:
            // 0 – общая;
            // 1 – упрощенная, доход;
            //2 – упрощенная, доход минус расход;
            //3 – единый налог на вмененный доход;
            //4 – единый сельскохозяйственный налог;
            //5 – патентная система налогообложения
            "tax_system": 0,
            
            // Валюта, язык обязательные поля
            "currency": 'RUB',
            "language": 'ru-RU',
        },
    }
}