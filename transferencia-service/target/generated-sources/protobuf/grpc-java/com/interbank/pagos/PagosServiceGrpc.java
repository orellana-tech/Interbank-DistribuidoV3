package com.interbank.pagos;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.62.2)",
    comments = "Source: pagos.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class PagosServiceGrpc {

  private PagosServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "pagos.PagosService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.interbank.pagos.PagosRequest,
      com.interbank.pagos.PagosResponse> getProcesarMovimientoMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "procesarMovimiento",
      requestType = com.interbank.pagos.PagosRequest.class,
      responseType = com.interbank.pagos.PagosResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.interbank.pagos.PagosRequest,
      com.interbank.pagos.PagosResponse> getProcesarMovimientoMethod() {
    io.grpc.MethodDescriptor<com.interbank.pagos.PagosRequest, com.interbank.pagos.PagosResponse> getProcesarMovimientoMethod;
    if ((getProcesarMovimientoMethod = PagosServiceGrpc.getProcesarMovimientoMethod) == null) {
      synchronized (PagosServiceGrpc.class) {
        if ((getProcesarMovimientoMethod = PagosServiceGrpc.getProcesarMovimientoMethod) == null) {
          PagosServiceGrpc.getProcesarMovimientoMethod = getProcesarMovimientoMethod =
              io.grpc.MethodDescriptor.<com.interbank.pagos.PagosRequest, com.interbank.pagos.PagosResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "procesarMovimiento"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.interbank.pagos.PagosRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.interbank.pagos.PagosResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PagosServiceMethodDescriptorSupplier("procesarMovimiento"))
              .build();
        }
      }
    }
    return getProcesarMovimientoMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static PagosServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PagosServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PagosServiceStub>() {
        @java.lang.Override
        public PagosServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PagosServiceStub(channel, callOptions);
        }
      };
    return PagosServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static PagosServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PagosServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PagosServiceBlockingStub>() {
        @java.lang.Override
        public PagosServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PagosServiceBlockingStub(channel, callOptions);
        }
      };
    return PagosServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static PagosServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PagosServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PagosServiceFutureStub>() {
        @java.lang.Override
        public PagosServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PagosServiceFutureStub(channel, callOptions);
        }
      };
    return PagosServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void procesarMovimiento(com.interbank.pagos.PagosRequest request,
        io.grpc.stub.StreamObserver<com.interbank.pagos.PagosResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getProcesarMovimientoMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service PagosService.
   */
  public static abstract class PagosServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return PagosServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service PagosService.
   */
  public static final class PagosServiceStub
      extends io.grpc.stub.AbstractAsyncStub<PagosServiceStub> {
    private PagosServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PagosServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PagosServiceStub(channel, callOptions);
    }

    /**
     */
    public void procesarMovimiento(com.interbank.pagos.PagosRequest request,
        io.grpc.stub.StreamObserver<com.interbank.pagos.PagosResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getProcesarMovimientoMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service PagosService.
   */
  public static final class PagosServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<PagosServiceBlockingStub> {
    private PagosServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PagosServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PagosServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.interbank.pagos.PagosResponse procesarMovimiento(com.interbank.pagos.PagosRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getProcesarMovimientoMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service PagosService.
   */
  public static final class PagosServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<PagosServiceFutureStub> {
    private PagosServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PagosServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PagosServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.interbank.pagos.PagosResponse> procesarMovimiento(
        com.interbank.pagos.PagosRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getProcesarMovimientoMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_PROCESAR_MOVIMIENTO = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_PROCESAR_MOVIMIENTO:
          serviceImpl.procesarMovimiento((com.interbank.pagos.PagosRequest) request,
              (io.grpc.stub.StreamObserver<com.interbank.pagos.PagosResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getProcesarMovimientoMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.interbank.pagos.PagosRequest,
              com.interbank.pagos.PagosResponse>(
                service, METHODID_PROCESAR_MOVIMIENTO)))
        .build();
  }

  private static abstract class PagosServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    PagosServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.interbank.pagos.Pagos.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("PagosService");
    }
  }

  private static final class PagosServiceFileDescriptorSupplier
      extends PagosServiceBaseDescriptorSupplier {
    PagosServiceFileDescriptorSupplier() {}
  }

  private static final class PagosServiceMethodDescriptorSupplier
      extends PagosServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    PagosServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (PagosServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new PagosServiceFileDescriptorSupplier())
              .addMethod(getProcesarMovimientoMethod())
              .build();
        }
      }
    }
    return result;
  }
}
