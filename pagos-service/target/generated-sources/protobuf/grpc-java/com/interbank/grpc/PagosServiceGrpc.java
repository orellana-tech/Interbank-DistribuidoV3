package com.interbank.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * Servicio de Pagos
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.60.0)",
    comments = "Source: interbank.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class PagosServiceGrpc {

  private PagosServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "com.interbank.grpc.PagosService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.interbank.grpc.PaymentRequest,
      com.interbank.grpc.PaymentResponse> getProcessPaymentMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ProcessPayment",
      requestType = com.interbank.grpc.PaymentRequest.class,
      responseType = com.interbank.grpc.PaymentResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.interbank.grpc.PaymentRequest,
      com.interbank.grpc.PaymentResponse> getProcessPaymentMethod() {
    io.grpc.MethodDescriptor<com.interbank.grpc.PaymentRequest, com.interbank.grpc.PaymentResponse> getProcessPaymentMethod;
    if ((getProcessPaymentMethod = PagosServiceGrpc.getProcessPaymentMethod) == null) {
      synchronized (PagosServiceGrpc.class) {
        if ((getProcessPaymentMethod = PagosServiceGrpc.getProcessPaymentMethod) == null) {
          PagosServiceGrpc.getProcessPaymentMethod = getProcessPaymentMethod =
              io.grpc.MethodDescriptor.<com.interbank.grpc.PaymentRequest, com.interbank.grpc.PaymentResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ProcessPayment"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.interbank.grpc.PaymentRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.interbank.grpc.PaymentResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PagosServiceMethodDescriptorSupplier("ProcessPayment"))
              .build();
        }
      }
    }
    return getProcessPaymentMethod;
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
   * <pre>
   * Servicio de Pagos
   * </pre>
   */
  public interface AsyncService {

    /**
     */
    default void processPayment(com.interbank.grpc.PaymentRequest request,
        io.grpc.stub.StreamObserver<com.interbank.grpc.PaymentResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getProcessPaymentMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service PagosService.
   * <pre>
   * Servicio de Pagos
   * </pre>
   */
  public static abstract class PagosServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return PagosServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service PagosService.
   * <pre>
   * Servicio de Pagos
   * </pre>
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
    public void processPayment(com.interbank.grpc.PaymentRequest request,
        io.grpc.stub.StreamObserver<com.interbank.grpc.PaymentResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getProcessPaymentMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service PagosService.
   * <pre>
   * Servicio de Pagos
   * </pre>
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
    public com.interbank.grpc.PaymentResponse processPayment(com.interbank.grpc.PaymentRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getProcessPaymentMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service PagosService.
   * <pre>
   * Servicio de Pagos
   * </pre>
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
    public com.google.common.util.concurrent.ListenableFuture<com.interbank.grpc.PaymentResponse> processPayment(
        com.interbank.grpc.PaymentRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getProcessPaymentMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_PROCESS_PAYMENT = 0;

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
        case METHODID_PROCESS_PAYMENT:
          serviceImpl.processPayment((com.interbank.grpc.PaymentRequest) request,
              (io.grpc.stub.StreamObserver<com.interbank.grpc.PaymentResponse>) responseObserver);
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
          getProcessPaymentMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.interbank.grpc.PaymentRequest,
              com.interbank.grpc.PaymentResponse>(
                service, METHODID_PROCESS_PAYMENT)))
        .build();
  }

  private static abstract class PagosServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    PagosServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.interbank.grpc.Interbank.getDescriptor();
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
              .addMethod(getProcessPaymentMethod())
              .build();
        }
      }
    }
    return result;
  }
}
