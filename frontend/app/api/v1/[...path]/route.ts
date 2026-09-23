import { NextRequest, NextResponse } from "next/server";

const backendUrl = process.env.BACKEND_URL ?? "http://localhost:8080";

async function proxy(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  const { path } = await context.params;
  const target = new URL(`/api/v1/${path.join("/")}`, backendUrl);
  target.search = request.nextUrl.search;

  try {
    const response = await fetch(target, {
      method: request.method,
      headers: {
        Accept: request.headers.get("accept") ?? "application/json",
        ...(request.headers.get("content-type")
          ? { "Content-Type": request.headers.get("content-type")! }
          : {}),
      },
      body: request.method === "GET" || request.method === "HEAD" ? undefined : await request.text(),
      cache: "no-store",
    });

    return new NextResponse(response.body, {
      status: response.status,
      headers: {
        "Content-Type": response.headers.get("content-type") ?? "application/json",
        ...(response.headers.get("location") ? { Location: response.headers.get("location")! } : {}),
      },
    });
  } catch {
    return NextResponse.json(
      {
        type: "about:blank",
        title: "Backend unavailable",
        status: 503,
        detail: "The ticket service could not be reached.",
      },
      { status: 503, headers: { "Content-Type": "application/problem+json" } },
    );
  }
}

export const GET = proxy;
export const POST = proxy;
export const PATCH = proxy;
