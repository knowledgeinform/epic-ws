# Please run from Epic-WS project root.

docker build . -t epic-ws
docker run -it --rm -p 8080:8080 epic-ws