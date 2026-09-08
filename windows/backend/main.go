// main.go — FsimeServer entry point.
// Cross-compiled from Ubuntu: CGO_ENABLED=0 GOOS=windows GOARCH=amd64 go build -o fsime-server.exe .
package main

import (
	"flag"
	"log"
	"os"
	"path/filepath"

	winio "github.com/Microsoft/go-winio"

	"github.com/wade/fsime-server/db"
	"github.com/wade/fsime-server/engine"
	"github.com/wade/fsime-server/server"
)

func main() {
	dbPath := flag.String("db", defaultDBPath(), "path to b.db SQLite database")
	flag.Parse()

	log.SetFlags(log.Ltime | log.Lshortfile)
	log.Printf("[main] Opening database: %s", *dbPath)

	bdb, err := db.Open(*dbPath)
	if err != nil {
		log.Fatalf("[main] Failed to open database: %v", err)
	}
	defer bdb.Close()

	proc := engine.New(bdb)

	// Create the Named Pipe listener.
	// SDDLs grant access to the current user and the TSF DLL (same user session).
	l, err := winio.ListenPipe(server.PipeName, &winio.PipeConfig{
		SecurityDescriptor: "D:P(A;;GA;;;WD)", // World: Generic All — tighten in production
		MessageMode:        false,
		InputBufferSize:    65536,
		OutputBufferSize:   65536,
	})
	if err != nil {
		log.Fatalf("[main] Failed to create Named Pipe: %v", err)
	}
	defer l.Close()

	server.Serve(l, proc)
}

// defaultDBPath returns a sensible default location for b.db on Windows.
// By convention, place b.db next to the .exe in the install directory.
func defaultDBPath() string {
	exe, err := os.Executable()
	if err != nil {
		return "b.db"
	}
	return filepath.Join(filepath.Dir(exe), "b.db")
}
