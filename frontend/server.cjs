const http = require("node:http");
const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname);
const port = Number(process.env.FRONTEND_PORT || 5500);
const contentTypes = {
  ".html": "text/html; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".svg": "image/svg+xml"
};

http.createServer((request, response) => {
  let pathname;
  try {
    pathname = decodeURIComponent(new URL(request.url, "http://localhost").pathname);
  } catch {
    response.writeHead(400).end("Invalid URL");
    return;
  }
  if (pathname === "/") pathname = "/index.html";
  const file = path.resolve(root, `.${pathname}`);
  if (path.relative(root, file).startsWith("..") || path.isAbsolute(path.relative(root, file))) {
    response.writeHead(403).end("Forbidden");
    return;
  }
  fs.readFile(file, (error, content) => {
    if (error) {
      response.writeHead(404, { "Content-Type": "text/html; charset=utf-8" });
      response.end(fs.readFileSync(path.join(root, "not-found.html")));
      return;
    }
    response.writeHead(200, {
      "Content-Type": contentTypes[path.extname(file)] || "application/octet-stream",
      "X-Content-Type-Options": "nosniff"
    });
    response.end(content);
  });
}).listen(port, () => console.log(`EventHub frontend available at http://localhost:${port}`));
