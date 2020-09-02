FROM python:3.8.5-slim-buster

WORKDIR /home/api-server/app

ENV PYTHONDONTWRITEBYTECODE 1

RUN apt-get update \
    && apt-get install -y locales \
    && rm -rf /var/lib/apt/lists/*

COPY . /home/api-server/app

RUN pip install -r requirements.txt

EXPOSE 80

CMD ["uvicorn", "src.main:app", "--host", "0.0.0.0", "--port", "80"]
