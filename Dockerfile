FROM clojure:tools-deps

WORKDIR /tmp/build

RUN curl -sL https://deb.nodesource.com/setup_12.x | bash - && \
    apt-get install -qq nodejs && \
    curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | apt-key add - && \
    echo "deb https://dl.yarnpkg.com/debian/ stable main" | tee /etc/apt/sources.list.d/yarn.list && \
    apt update && apt install -qq yarn && \
    npm install -g shadow-cljs

COPY deps.edn .
RUN clojure -Spath > /dev/null

COPY package.json yarn.lock ./
RUN yarn install

COPY shadow-cljs.edn .
RUN shadow-cljs > /dev/null

COPY . .
RUN shadow-cljs release :main && \
    clojure -Spom && clojure -A:release > /dev/null && \
    mkdir /app && \
    mv /tmp/build/home.jar /app/home.jar && \
    rm -rf /tmp/build

WORKDIR /app

CMD ["java", "-jar", "home.jar", "-m", "home.core"]
